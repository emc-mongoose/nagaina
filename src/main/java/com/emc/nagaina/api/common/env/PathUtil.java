package com.emc.nagaina.api.common.env;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 Created by kurila on 03.11.16.
 */
public interface PathUtil {

	String BASE_DIR = getBaseDir();

	static String getBaseDir() {
		return getBasePathForClass(PathUtil.class);
	}

	static URI getBaseUriForClass(final Class<?> cls)
		throws URISyntaxException {
		return cls.getProtectionDomain().getCodeSource().getLocation().toURI();
	}

	// http://stackoverflow.com/a/29665447
	static String getBasePathForClass(final Class<?> cls) {
		try {
			File basePath;
			final File clsFile = new File(getBaseUriForClass(cls).getPath());
			if(
				!clsFile.isDirectory() || clsFile.getPath().endsWith(".jar") ||
					clsFile.getPath().endsWith(".zip")
				) {
				basePath = clsFile.getParentFile();
			} else {
				basePath = clsFile;
			}
			String basePathStr = basePath.toString();
			// bandage for eclipse
			if(
				basePathStr.endsWith(File.separator + "lib") ||
					basePathStr.endsWith(File.separator + "bin") ||
					basePathStr.endsWith("bin" + File.separator) ||
					basePathStr.endsWith("lib" + File.separator)
				) {
				basePath = basePath.getParentFile();
			}
			// bandage for netbeans
			if(basePathStr.endsWith(File.separator + "build" + File.separator + "classes")) {
				basePath = basePath
					.getParentFile()
					.getParentFile();
			}
			// bandage for idea
			if(
				basePathStr.endsWith(
					File.separator + "build" + File.separator + "classes" + File.separator +
						"java" + File.separator + "main"
				)
			) {
				basePath = basePath
					.getParentFile()
					.getParentFile()
					.getParentFile()
					.getParentFile();
			}
			// another bandage for idea (2017.2)
			if(
				basePathStr.endsWith(
					File.separator + "out" + File.separator + "production" + File.separator
						+ "classes"
				)
			) {
				basePath = basePath
					.getParentFile()
					.getParentFile()
					.getParentFile();
			}
			// bandage for gradle
			if(
				basePathStr.endsWith(
					File.separator + "build" + File.separator + "classes" + File.separator + "main"
				)
			) {
				basePath = basePath
					.getParentFile()
					.getParentFile()
					.getParentFile();
			}
			// another bandage for gradle
			if(basePathStr.endsWith(File.separator + "build" + File.separator + "libs")) {
				basePath = basePath
					.getParentFile()
					.getParentFile();
			}

			return basePath.toString();
		} catch(final URISyntaxException e) {
			throw new RuntimeException("Cannot figure out base path for class: " + cls.getName());
		}
	}
}
