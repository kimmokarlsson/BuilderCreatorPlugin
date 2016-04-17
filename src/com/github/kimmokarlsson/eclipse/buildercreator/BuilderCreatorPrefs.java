package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.jface.preference.IPreferenceStore;

public class BuilderCreatorPrefs {

	public static final String PREF_PREFIX = BuilderCreatorPrefs.class.getPackage().getName() + ".prefs.";
	public static final String PREF_CONVERT_FIELDS = "convertFieldsPrivate";
	public static final String PREF_CREATE_BUILDERFROM_METHOD = "builderFromMethod";
	public static final String PREF_JACKSON_ANNOTATIONS = "jacksonAnnotations";

	private BuilderCreatorPrefs() {
	}

	public static String getQualifiedName(String name) {
		return PREF_PREFIX + name;
	}

	public static boolean getBoolean(String name) {
		return getPrefs().getBoolean(PREF_PREFIX + name);
	}

	public static String getString(String name) {
		return getPrefs().getString(PREF_PREFIX + name);
	}

	private static IPreferenceStore getPrefs() {
		return BuilderCreatorPlugin.getDefault().getPreferenceStore();
	}
}
