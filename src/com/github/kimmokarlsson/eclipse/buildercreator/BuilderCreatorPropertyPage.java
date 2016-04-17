package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Property page in the project properties.
 */
public class BuilderCreatorPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

    private static final String PROP_KEY = "projectPropertyName";

	private Button[] checkboxes;
	private IProject project;

	public BuilderCreatorPropertyPage() {
	    super();
	}

	@Override
	protected Control createContents(Composite parent) {
        IAdaptable resource = getElement();
        project = resource.getAdapter(IProject.class);

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);

        checkboxes = new Button[3];
        checkboxes[0] = new Button(composite, SWT.CHECK);
        checkboxes[0].setLayoutData(new GridData());
        checkboxes[0].setText("Convert fields to private final");
        checkboxes[0].setData(PROP_KEY, BuilderCreatorPrefs.PREF_CONVERT_FIELDS);

        checkboxes[1] = new Button(composite, SWT.CHECK);
        checkboxes[1].setLayoutData(new GridData());
        checkboxes[1].setText("Create builderFrom() method");
        checkboxes[1].setData(PROP_KEY, BuilderCreatorPrefs.PREF_CREATE_BUILDERFROM_METHOD);

        checkboxes[2] = new Button(composite, SWT.CHECK);
        checkboxes[2].setLayoutData(new GridData());
        checkboxes[2].setText("Add Jackson Annotations");
        checkboxes[2].setData(PROP_KEY, BuilderCreatorPrefs.PREF_JACKSON_ANNOTATIONS);

        // initialize values
        for (Button b : checkboxes) {
            b.setSelection(getBooleanProperty((String)b.getData(PROP_KEY)));
        }

		return composite;
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

	@Override
	public boolean performOk() {
	    for (Button b : checkboxes) {
	        setBooleanProperty((String)b.getData(PROP_KEY), b.getSelection());
	    }
		return true;
	}

	private void setBooleanProperty(String localName, boolean value) {
        try {
			project.setPersistentProperty(new QualifiedName(BuilderCreatorPrefs.PREF_PREFIX, localName), Boolean.toString(value));
		} catch (CoreException e) {
		    ErrorLog.error("Setting project property", e);
		}
	}
}
