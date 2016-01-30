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

	private String getPrefName(String suffix) {
		return getClass().getPackage().getName() + suffix;
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(getPrefName(".prefs.privateFinal"),
				"Convert fields into private final",
				SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(getPrefName(".prefs.builderFrom"),
				"Create builderFrom Method",
				SWT.NONE, getFieldEditorParent()));
		addField(new BooleanFieldEditor(getPrefName(".prefs.jackson"),
				"Add Jackson Annotations",
				SWT.NONE, getFieldEditorParent()));
	}
}
