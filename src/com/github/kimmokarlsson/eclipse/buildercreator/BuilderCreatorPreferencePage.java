package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
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
				SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(BuilderCreatorPrefs.getQualifiedName(BuilderCreatorPrefs.PREF_CREATE_BUILDERFROM_METHOD),
				"Create builderFrom Method",
				SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(BuilderCreatorPrefs.getQualifiedName(BuilderCreatorPrefs.PREF_JACKSON_ANNOTATIONS),
				"Add Jackson Annotations",
				SWT.NONE, getFieldEditorParent()));
	}
}
