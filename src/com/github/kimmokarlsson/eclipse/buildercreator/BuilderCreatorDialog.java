package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class BuilderCreatorDialog extends AbstractModalDialog {

	private int status = -1;
	private List<IField> selectedFields;
	private IField lastField;
	private BuilderCodeGenerator.Settings settings;
	private Collection<TableItem> fieldButtons;
	private Collection<IField> allFields;

	public BuilderCreatorDialog() {
		super(BuilderCreatorPlugin.getDefault().getWorkbench().getModalDialogShellProvider().getShell(), SWT.RESIZE);
		selectedFields = new ArrayList<>();
	}

	public BuilderCodeGenerator.Settings getSettings() {
		return settings;
	}

	public List<IField> getFields() {
		return selectedFields;
	}

	public IField getLastField() {
		return lastField;
	}

	public int show(ICompilationUnit compilationUnit) throws JavaModelException {
		final Shell shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.CENTER | SWT.RESIZE);
        shell.setText("Generate Builder");
        shell.setLayout(new GridLayout(2, false));

        Label classGroup = new Label(shell, SWT.NONE);
        classGroup.setText("Select (sub)-class:");
        GridData classGroupLayoutData = new GridData();
        classGroupLayoutData.horizontalSpan = 2;
        classGroup.setLayoutData(classGroupLayoutData);

        final Combo classCombo = new Combo(shell, SWT.BORDER);
        GridData classComboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        classComboData.horizontalSpan = 2;
        classCombo.setLayoutData(classComboData);
        for (String s : BuilderCodeGenerator.findAllClasses(compilationUnit)) {
        	classCombo.add(s);
        }
        classCombo.select(0);

        Label fieldGroup = new Label(shell, SWT.NONE);
        fieldGroup.setText("Select fields to include:");
        GridData fieldGroupLayoutData = new GridData();
        fieldGroupLayoutData.horizontalSpan = 2;
        fieldGroup.setLayoutData(fieldGroupLayoutData);

        final Table fieldTable = new Table(shell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData fieldTableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        fieldTableData.verticalSpan = 5;
        fieldTable.setLayoutData(fieldTableData);

        resetTableContents(compilationUnit, fieldTable, null);
        classCombo.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent event) {
        		int index = classCombo.getSelectionIndex();
        		String className = classCombo.getItem(index);
        		try {
					resetTableContents(compilationUnit, fieldTable, className);
				} catch (JavaModelException e) {
					ErrorLog.error("Resetting dialog's table component", e);
				}
        	}});

        Button btnSelectAll = new Button(shell, SWT.PUSH);
        btnSelectAll.setText("Select All");
        GridData btnSelectAllLayoutData = new GridData();
        btnSelectAllLayoutData.grabExcessHorizontalSpace = true;
        btnSelectAllLayoutData.minimumWidth = 150;
        btnSelectAll.setLayoutData(btnSelectAllLayoutData);
        btnSelectAll.addSelectionListener(new SelectionAdapter() {
            @Override
        	public void widgetSelected(SelectionEvent event) {
                for (TableItem button : fieldButtons) {
                    button.setChecked(true);
                }
            }});
        Button btnSelectNone = new Button(shell, SWT.PUSH);
        btnSelectNone.setText("Deselect All");
        GridData selectNoneGridData = new GridData();
        selectNoneGridData.grabExcessHorizontalSpace = true;
        selectNoneGridData.minimumWidth = 150;
        btnSelectNone.setLayoutData(selectNoneGridData);
        btnSelectNone.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                for (TableItem button : fieldButtons) {
                    button.setChecked(false);
                }
            }});

        Group optionGroup = new Group(shell, SWT.SHADOW_ETCHED_IN);
        optionGroup.setText("Options:");
        optionGroup.setLayout(new RowLayout(SWT.VERTICAL));
        GridData optionGridData = new GridData();
        optionGridData.horizontalSpan = 2;
        optionGridData.horizontalAlignment = SWT.FILL;
        optionGroup.setLayoutData(optionGridData);

        IProject project = compilationUnit.getParent().getJavaProject().getAdapter(IProject.class);

        final Button convertFieldsToFinal = new Button(optionGroup, SWT.CHECK);
        convertFieldsToFinal.setSelection(getBooleanProperty(project, BuilderCreatorPrefs.PREF_CONVERT_FIELDS));
        convertFieldsToFinal.setText("Convert fields to private final");

        final Button createBuilderFromMethod = new Button(optionGroup, SWT.CHECK);
        createBuilderFromMethod.setSelection(getBooleanProperty(project, BuilderCreatorPrefs.PREF_CREATE_BUILDERFROM_METHOD));
        createBuilderFromMethod.setText("Create builderFrom() method");

        final Button createJacksonAnnotations = new Button(optionGroup, SWT.CHECK);
        createJacksonAnnotations.setSelection(getBooleanProperty(project, BuilderCreatorPrefs.PREF_JACKSON_ANNOTATIONS));
        createJacksonAnnotations.setText("Create Jackson annotations");

        final Button createEqualsMethod = new Button(optionGroup, SWT.CHECK);
        createEqualsMethod.setSelection(getBooleanProperty(project, BuilderCreatorPrefs.PREF_EQUALS_METHOD));
        createEqualsMethod.setText("Create equals() and hashCode() methods");

        final Label builderLabel = new Label(optionGroup, SWT.NONE);
        builderLabel.setText("Builder method prefix:");
        final Text builderMethodPrefix = new Text(optionGroup, SWT.BORDER);
        builderMethodPrefix.setMessage("Builder field setter method prefix");
        builderMethodPrefix.setText(getStringProperty(project, BuilderCreatorPrefs.PREF_BUILDER_PREFIX));

        final Label methodLabel = new Label(optionGroup, SWT.NONE);
        methodLabel.setText("Builder method name:");
        final Text buildMethodName = new Text(optionGroup, SWT.BORDER);
        buildMethodName.setMessage("Build method name");
        buildMethodName.setText(getStringProperty(project, BuilderCreatorPrefs.PREF_BUILDER_METHOD_NAME));

        Group buttonContainer = new Group(shell, SWT.SHADOW_NONE);
        buttonContainer.setLayout(new GridLayout(2, false));
        GridData containerData = new GridData();
        containerData.horizontalSpan = 2;
        containerData.horizontalAlignment = SWT.RIGHT;
        buttonContainer.setLayoutData(containerData);

        final Button cancelButton = new Button(buttonContainer, SWT.PUSH);
        GridData cancelGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        cancelGridData.minimumWidth = 150;
        cancelGridData.grabExcessHorizontalSpace = true;
        cancelButton.setLayoutData(cancelGridData);
        cancelButton.setText("Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				status = Dialog.CANCEL;
				shell.dispose();
			}
		});

        final Button executeButton = new Button(buttonContainer, SWT.PUSH);
        GridData executeGridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        executeGridData.minimumWidth = 150;
        executeButton.setLayoutData(executeGridData);
        executeButton.setText("OK");
        executeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				status = Dialog.OK;
				Iterator<IField> fieldIter = allFields.iterator();
				for (TableItem button : fieldButtons) {
					IField f = fieldIter.next();
					if (button.getChecked()) {
						selectedFields.add(f);
					}
				}
				settings = BuilderCodeGenerator.Settings.builder()
						.builderFromMethod(createBuilderFromMethod.getSelection())
						.convertFieldsFinal(convertFieldsToFinal.getSelection())
						.jacksonAnnotations(createJacksonAnnotations.getSelection())
						.equalsMethod(createEqualsMethod.getSelection())
						.buildMethodName(buildMethodName.getText())
						.methodPrefix(builderMethodPrefix.getText())
						.build();
				shell.dispose();
			}
		});

        optionGroup.pack();
        display(shell);
		return status;
	}

	private void resetTableContents(ICompilationUnit compilationUnit, Table fieldTable, String className) throws JavaModelException {
        fieldTable.removeAll();
        allFields = BuilderCodeGenerator.findAllFields(compilationUnit, className);
        fieldButtons = new ArrayList<>();
        for (IField field : allFields) {
            TableItem item = new TableItem(fieldTable, SWT.NONE);
            item.setText(BuilderCodeGenerator.getFieldType(field) + " " + field.getElementName());
            item.setData(field);
            item.setChecked(true);
            fieldButtons.add(item);
            lastField = field;
        }
	}

	private boolean getBooleanProperty(IProject project, String localName) {
	    try {
			String prop = project.getPersistentProperty(new QualifiedName(BuilderCreatorPrefs.PREF_PREFIX, localName));
			if (prop != null) {
				return Boolean.parseBoolean(prop);
			}
			return BuilderCreatorPrefs.getBoolean(localName);
		} catch (CoreException e) {
		    ErrorLog.error("Reading project property", e);
		}
	    return false;
	}

	private String getStringProperty(IProject project, String localName) {
	    try {
			String prop = project.getPersistentProperty(new QualifiedName(BuilderCreatorPrefs.PREF_PREFIX, localName));
			if (prop != null) {
				return prop;
			}
			return BuilderCreatorPrefs.getString(localName);
		} catch (CoreException e) {
		    ErrorLog.error("Reading project property", e);
		}
	    return null;
	}
}
