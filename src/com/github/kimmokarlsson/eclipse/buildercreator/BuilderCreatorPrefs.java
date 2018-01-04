package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;

import java.io.IOException;

public class BuilderCreatorPrefs {

	public static final String PREF_PREFIX = BuilderCreatorPrefs.class.getPackage().getName() + ".prefs.";
	public static final String PREF_CONVERT_FIELDS = "convertFieldsPrivate";
	public static final String PREF_CREATE_BUILDERFROM_METHOD = "builderFromMethod";
	public static final String PREF_JACKSON_ANNOTATIONS = "jacksonAnnotations";
	public static final String PREF_BUILDER_METHOD_NAME = "builderMethodName";
	public static final String PREF_BUILDER_PREFIX = "builderClassMethodPrefix";
    public static final String PREF_EQUALS_METHOD = "equalsMethod";

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

	public static void setDefaults() {
		String b = getString(PREF_BUILDER_METHOD_NAME);
		if (b == null || b.length() == 0) {
			IPreferenceStore prefs = getPrefs();
			prefs.setDefault(getQualifiedName(PREF_BUILDER_METHOD_NAME), "build");
			prefs.setValue(getQualifiedName(PREF_BUILDER_METHOD_NAME), "build");
			prefs.setDefault(getQualifiedName(PREF_BUILDER_PREFIX), "");
			prefs.setValue(getQualifiedName(PREF_BUILDER_PREFIX), "");
			prefs.setDefault(getQualifiedName(PREF_CONVERT_FIELDS), true);
			prefs.setValue(getQualifiedName(PREF_CONVERT_FIELDS), true);
			prefs.setDefault(getQualifiedName(PREF_CREATE_BUILDERFROM_METHOD), true);
			prefs.setValue(getQualifiedName(PREF_CREATE_BUILDERFROM_METHOD), true);
			prefs.setDefault(getQualifiedName(PREF_JACKSON_ANNOTATIONS), true);
			prefs.setValue(getQualifiedName(PREF_JACKSON_ANNOTATIONS), true);
			if (prefs instanceof IPersistentPreferenceStore) {
				try {
					((IPersistentPreferenceStore)prefs).save();
				} catch (IOException e) {
					ErrorLog.error("Saving Preferences", e);
				}
			}
		}
	}
}
