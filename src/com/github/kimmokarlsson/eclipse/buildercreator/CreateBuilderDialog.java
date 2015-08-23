package com.github.kimmokarlsson.eclipse.buildercreator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class CreateBuilderDialog extends AbstractModalDialog {

	private int status = -1;
	private List<IField> selectedFields;
	private IField lastField;
	private BuilderCodeGenerator.Settings settings;
    
	public CreateBuilderDialog() {
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

        Label fieldGroup = new Label(shell, SWT.NONE);
        fieldGroup.setText("Select fields to include:");
        GridData fieldGroupLayoutData = new GridData();
        fieldGroupLayoutData.horizontalSpan = 2;
        fieldGroup.setLayoutData(fieldGroupLayoutData);

        Table fieldTable = new Table(shell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData fieldTableData = new GridData(SWT.FILL, SWT.FILL, true, true);
        fieldTableData.verticalSpan = 5;
        fieldTable.setLayoutData(fieldTableData);
        
        Collection<IField> fields = BuilderCodeGenerator.findAllFIelds(compilationUnit);
        final Collection<TableItem> fieldButtons = new ArrayList<>();
        for (IField field : fields) {
            TableItem item = new TableItem(fieldTable, SWT.NONE);
            item.setText(BuilderCodeGenerator.getFieldType(field) + " " + field.getElementName());
            item.setData(field);
            item.setChecked(true);
            fieldButtons.add(item);
            lastField = field;
        }
        
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

        final Button convertFieldsToFinal = new Button(optionGroup, SWT.CHECK);
        convertFieldsToFinal.setSelection(true);
        convertFieldsToFinal.setText("Convert fields to private final");

        final Button createBuilderFromMethod = new Button(optionGroup, SWT.CHECK);
        createBuilderFromMethod.setSelection(true);
        createBuilderFromMethod.setText("Create builderFrom method");

        final Label builderLabel = new Label(optionGroup, SWT.NONE);
        builderLabel.setText("Builder method prefix:");
        final Text builderMethodPrefix = new Text(optionGroup, SWT.BORDER);
        builderMethodPrefix.setMessage("Builder field setter method prefix");

        final Label methodLabel = new Label(optionGroup, SWT.NONE);
        methodLabel.setText("Builder method name:");
        final Text buildMethodName = new Text(optionGroup, SWT.BORDER);
        buildMethodName.setMessage("Build method name");
        buildMethodName.setText("build");

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
				Iterator<IField> fieldIter = fields.iterator();
				for (TableItem button : fieldButtons) {
					IField f = fieldIter.next();
					if (button.getChecked()) {
						selectedFields.add(f);
					}
				}
				settings = BuilderCodeGenerator.Settings.builder()
						.builderFromMethod(createBuilderFromMethod.getSelection())
						.convertFieldsFinal(convertFieldsToFinal.getSelection())
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
}
