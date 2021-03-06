package com.emc.nagaina.impl.base;

import com.github.akurilov.commons.concurrent.AnyNotNullSharedFutureTaskBase;
import com.github.akurilov.commons.concurrent.ThreadUtil;

import com.emc.mongoose.api.model.concurrent.DaemonBase;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.api.model.concurrent.LogContextThreadFactory;
import com.emc.mongoose.ui.log.Loggers;

import com.emc.nagaina.api.DataItemMock;
import com.emc.nagaina.api.StorageMockClient;
import com.emc.nagaina.api.StorageMockServer;
import com.emc.nagaina.api.exception.ContainerMockException;
import com.emc.nagaina.impl.remote.MDns;
import static com.emc.nagaina.impl.http.WeightlessHttpStorageMock.SVC_NAME;

import org.apache.logging.log4j.Level;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import static java.rmi.registry.Registry.REGISTRY_PORT;

/**
 Created on 06.09.16.
 */
public final class BasicStorageMockClient<T extends DataItemMock>
extends DaemonBase
implements StorageMockClient<T> {

	private final DataInput contentSrc;
	private final JmDNS jmDns;
	private final Map<URI, StorageMockServer<T>> remoteNodeMap = new ConcurrentHashMap<>();
	private final ExecutorService executor;

	public BasicStorageMockClient(final DataInput dataInput, final JmDNS jmDns) {
		this.executor = new ThreadPoolExecutor(
			ThreadUtil.getHardwareThreadCount(), ThreadUtil.getHardwareThreadCount(),
			0, TimeUnit.DAYS, new ArrayBlockingQueue<>(0x1000),
			new LogContextThreadFactory("storageMockClientWorker", true),
			(r, e) -> Loggers.ERR.error("Task {} rejected", r.toString())
		) {
			@Override
			public final Future<T> submit(final Runnable task) {
				final RunnableFuture<T> rf = (RunnableFuture<T>) task;
				execute(rf);
				return rf;
			}
		};
		this.contentSrc = dataInput;
		this.jmDns = jmDns;
	}
	
	private static final class GetRemoteObjectTask<T extends DataItemMock>
	extends AnyNotNullSharedFutureTaskBase<T> {
		
		private final StorageMockServer<T> node;
		private final String containerName;
		private final String id;
		private final long offset;
		private final long size;
		
		public GetRemoteObjectTask(
			final AtomicReference<T> resultRef, final CountDownLatch sharedLatch,
			final StorageMockServer<T> node, final String containerName, final String id,
			final long offset, final long size
		) {
			super(resultRef, sharedLatch);
			this.node = node;
			this.containerName = containerName;
			this.id = id;
			this.offset = offset;
			this.size = size;
		}
		
		@Override
		public final void run() {
			try {
				final T remoteObject = node.getObjectRemotely(containerName, id, offset, size);
				set(remoteObject);
			} catch(final ContainerMockException | RemoteException e) {
				setException(e);
			}
		}
	}
	
	@Override
	public T getObject(
		final String containerName, final String id, final long offset, final long size
	) throws ExecutionException, InterruptedException {
		final Collection<StorageMockServer<T>> remoteNodes = remoteNodeMap.values();
		final CountDownLatch sharedCountDown = new CountDownLatch(remoteNodes.size());
		final AtomicReference<T> resultRef = new AtomicReference<>(null);
		for(final StorageMockServer<T> node : remoteNodes) {
			executor.submit(
				new GetRemoteObjectTask<>(
					resultRef, sharedCountDown, node, containerName, id, offset, size
				)
			);
		}
		T result;
		while(null == (result = resultRef.get()) && sharedCountDown.getCount() > 0) {
			Thread.sleep(1);
		}
		if(result != null) {
			result.setDataInput(contentSrc);
		}
		return result;
	}
	
	@Override
	protected void doStart() {
		jmDns.addServiceListener(MDns.Type.HTTP.toString(), this);
	}
	
	@Override
	protected void doShutdown() {
		executor.shutdown();
	}
	
	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		return executor.awaitTermination(timeout, timeUnit);
	}
	
	@Override
	protected void doInterrupt() {
		executor.shutdownNow();
		jmDns.removeServiceListener(MDns.Type.HTTP.toString(), this);
	}
	
	@Override
	protected void doClose()
	throws IOException {
		remoteNodeMap.clear();
	}

	@Override
	public void serviceAdded(final ServiceEvent event) {
		jmDns.requestServiceInfo(event.getType(), event.getName(), 10);
	}

	@Override
	public void serviceRemoved(final ServiceEvent event) {
		handleServiceEvent(event, remoteNodeMap::remove, "Node removed");
	}

	@Override @SuppressWarnings("unchecked")
	public void serviceResolved(final ServiceEvent event) {
		final Consumer<String> c = hostAddress -> {
			try {
				final URI rmiUrl = new URI(
					"rmi", null, hostAddress, REGISTRY_PORT, "/" + SVC_NAME, null, null
				);
				final StorageMockServer<T> mock = (StorageMockServer<T>) Naming.lookup(rmiUrl.toString());
				remoteNodeMap.putIfAbsent(rmiUrl, mock);
			} catch(final NotBoundException | MalformedURLException | RemoteException e) {
				LogUtil.exception(Level.ERROR, e, "Failed to lookup node");
			} catch(final URISyntaxException e) {
				Loggers.ERR.debug("RMI URL syntax error {}", e);
			}
		};
		handleServiceEvent(event, c, "Node added");
	}

	private void handleServiceEvent(
		final ServiceEvent event, final Consumer<String> consumer, final String actionMsg
	) {
		final ServiceInfo eventInfo = event.getInfo();
		final String evtSvcName = eventInfo.getQualifiedName();
		if(evtSvcName.startsWith(SVC_NAME)) {
			for(final InetAddress address: eventInfo.getInet4Addresses()) {
				consumer.accept(address.getHostAddress());
			}
		}
	}
}
