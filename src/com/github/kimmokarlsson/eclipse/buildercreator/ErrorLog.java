package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ErrorLog {

	private ErrorLog() {
	}

	public static void error(String msg, Throwable t) {
		log(Status.ERROR, msg, t);
	}

	public static void warn(String msg, Throwable t) {
		log(Status.WARNING, msg, t);
	}

	public static void info(String msg, Throwable t) {
		log(Status.INFO, msg, t);
	}

	protected static void log(int severity, String msg, Throwable t) {
		IStatus status = new Status(severity, BuilderCreatorPlugin.PLUGIN_ID, msg, t);
		BuilderCreatorPlugin.getDefault().getLog().log(status);
	}
}
