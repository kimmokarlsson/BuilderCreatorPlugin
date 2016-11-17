package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import java.util.ArrayList;
import java.util.List;

/**
 * Property page in the project properties.
 */
public class BuilderCreatorPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

    private static final String PROP_KEY = "projectPropertyName";
	private static final String PROP_PROJECT_SPEC = "projectSpecificSettings";

	private IProject project;
	private Button[] checkboxes;
	private Text[] textboxes;
	private List<Label> labels;

	public BuilderCreatorPropertyPage() {
	    super();
	}

	private void openPreferencePage() {
		IPreferencePage page = new BuilderCreatorPreferencePage();
		PreferenceManager mgr = new PreferenceManager();
		IPreferenceNode node = new PreferenceNode(BuilderCreatorPrefs.PREF_PREFIX+"dialog", page);
		mgr.addToRoot(node);
		PreferenceDialog dialog = new PreferenceDialog(getShell(), mgr);
		dialog.create();
		dialog.setMessage(page.getTitle());
		dialog.open();
	}

	@Override
	protected Control createContents(Composite parent) {
        IAdaptable resource = getElement();
        project = resource.getAdapter(IProject.class);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		Composite topRow = new Composite(composite, SWT.NONE);
		topRow.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout topRowLayout = new GridLayout();
		topRowLayout.numColumns = 3;
		topRow.setLayout(topRowLayout);

        checkboxes = new Button[4];
        checkboxes[0] = new Button(topRow, SWT.CHECK);
        checkboxes[0].setLayoutData(new GridData());
        checkboxes[0].setText("Enable project specific settings");
        checkboxes[0].setData(PROP_KEY, PROP_PROJECT_SPEC);
        boolean initialEnabled = getBooleanProperty(PROP_PROJECT_SPEC);

        Composite spacer = new Composite(topRow, SWT.NONE);
        GridData spacerData = new GridData(GridData.FILL_HORIZONTAL);
        spacerData.heightHint = 8;
        spacerData.minimumHeight = 8;
        spacerData.minimumWidth = 8;
        spacer.setLayoutData(spacerData);

        Link prefLink = new Link(topRow, SWT.NONE);
        prefLink.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
        //prefLink.setForeground(new Color(parent.getDisplay(), new RGB(0,0,255)));
        prefLink.setText("<a href=\"pref\">Configure Workspace Settings...</a>");
        prefLink.addSelectionListener(new SelectionAdapter() {
        	@Override
			public void widgetSelected(SelectionEvent e) {
        		openPreferencePage();
        	}});
        prefLink.setEnabled(!initialEnabled);

        checkboxes[0].addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (int i = 1; i < checkboxes.length; i++) {
					checkboxes[i].setEnabled(!checkboxes[i].getEnabled());
				}
				for (Text t : textboxes) {
					t.setEnabled(!t.getEnabled());
				}
				for (Label a : labels) {
					a.setEnabled(!a.getEnabled());
				}
				prefLink.setEnabled(!prefLink.getEnabled());
			}});

        Group line = new Group(composite, SWT.NONE);
        GridData lineData = new GridData(GridData.FILL_HORIZONTAL);
        lineData.heightHint = 0;
        lineData.minimumHeight = 0;
        line.setLayoutData(lineData);

        Group group = new Group(composite, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setLayout(new GridLayout());
        group.setText("Toggles");

        checkboxes[1] = new Button(group, SWT.CHECK);
        checkboxes[1].setLayoutData(new GridData());
        checkboxes[1].setText("Convert fields to private final");
        checkboxes[1].setData(PROP_KEY, BuilderCreatorPrefs.PREF_CONVERT_FIELDS);
        checkboxes[1].setEnabled(initialEnabled);

        checkboxes[2] = new Button(group, SWT.CHECK);
        checkboxes[2].setLayoutData(new GridData());
        checkboxes[2].setText("Create builderFrom() method");
        checkboxes[2].setData(PROP_KEY, BuilderCreatorPrefs.PREF_CREATE_BUILDERFROM_METHOD);
        checkboxes[2].setEnabled(initialEnabled);

        checkboxes[3] = new Button(group, SWT.CHECK);
        checkboxes[3].setLayoutData(new GridData());
        checkboxes[3].setText("Add Jackson Annotations");
        checkboxes[3].setData(PROP_KEY, BuilderCreatorPrefs.PREF_JACKSON_ANNOTATIONS);
        checkboxes[3].setEnabled(initialEnabled);

        Group textGroup = new Group(composite, SWT.NONE);
        textGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout textLayout = new GridLayout();
        textLayout.numColumns = 2;
        textGroup.setLayout(textLayout);
        textGroup.setText("Names");

        labels = new ArrayList<>();
        textboxes = new Text[2];
        textboxes[0] = addTextProperty(textGroup, "Builder Method Name", BuilderCreatorPrefs.PREF_BUILDER_METHOD_NAME, initialEnabled);
        textboxes[1] = addTextProperty(textGroup, "Builder Class Method Prefix", BuilderCreatorPrefs.PREF_BUILDER_PREFIX, initialEnabled);

        // initialize values
        for (Button b : checkboxes) {
            b.setSelection(getBooleanProperty((String)b.getData(PROP_KEY)));
        }

		return composite;
	}

	private Text addTextProperty(Composite parent, String labelText, String prop, boolean initialEnabled) {

        Label label1 = new Label(parent, SWT.NONE);
        label1.setLayoutData(new GridData());
        label1.setText(labelText);
        label1.setEnabled(initialEnabled);
        labels.add(label1);

        Text field = new Text(parent, SWT.SINGLE | SWT.BORDER);
        field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        field.setData(PROP_KEY, prop);
        field.setEnabled(initialEnabled);
        field.setTextLimit(100);
        field.setText(getStringProperty(prop));

        return field;
	}

	private String getStringProperty(String localName) {
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

	private void setStringProperty(String localName, String value) {
        try {
			project.setPersistentProperty(new QualifiedName(BuilderCreatorPrefs.PREF_PREFIX, localName), value);
		} catch (CoreException e) {
		    ErrorLog.error("Setting project property", e);
		}
	}

	private boolean getBooleanProperty(String localName) {
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

	private void setBooleanProperty(String localName, boolean value) {
        try {
			project.setPersistentProperty(new QualifiedName(BuilderCreatorPrefs.PREF_PREFIX, localName), Boolean.toString(value));
		} catch (CoreException e) {
		    ErrorLog.error("Setting project property", e);
		}
	}

	@Override
	public boolean performOk() {
		if (checkboxes[0].getSelection()) {
		    for (Button b : checkboxes) {
		        setBooleanProperty((String)b.getData(PROP_KEY), b.getSelection());
		    }
		    for (Text t : textboxes) {
		    	setStringProperty((String)t.getData(PROP_KEY), t.getText());
		    }
		}
		else {
		    for (Button b : checkboxes) {
		        String key = (String)b.getData(PROP_KEY);
				setStringProperty(key, null);
		        b.setSelection(getBooleanProperty(key));
		    }
		    for (Text t : textboxes) {
		    	String key = (String)t.getData(PROP_KEY);
				setStringProperty(key, null);
				t.setText(getStringProperty(key));
		    }
		}
		return true;
	}
}
