package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page.
 */
public class BuilderCreatorPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public BuilderCreatorPreferencePage() {
		super("Builder Creator", GRID);
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return BuilderCreatorPlugin.getDefault().getPreferenceStore();
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(BuilderCreatorPrefs.getQualifiedName(BuilderCreatorPrefs.PREF_CONVERT_FIELDS),
				"Convert fields into private final",
				BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor(BuilderCreatorPrefs.getQualifiedName(BuilderCreatorPrefs.PREF_CREATE_BUILDERFROM_METHOD),
				"Create builderFrom Method",
				BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor(BuilderCreatorPrefs.getQualifiedName(BuilderCreatorPrefs.PREF_JACKSON_ANNOTATIONS),
				"Add Jackson Annotations",
				BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new StringFieldEditor(BuilderCreatorPrefs.getQualifiedName(BuilderCreatorPrefs.PREF_BUILDER_METHOD_NAME),
				"Builder Method Name",
				StringFieldEditor.UNLIMITED, getFieldEditorParent()));
		addField(new StringFieldEditor(BuilderCreatorPrefs.getQualifiedName(BuilderCreatorPrefs.PREF_BUILDER_PREFIX),
				"Builder Class Method Prefix",
				StringFieldEditor.UNLIMITED, getFieldEditorParent()));
	}
}
