package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

public class BuilderCreatorPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);

		/*Label label = new Label(composite, SWT.NONE);
		label.setText("Convert fields to private final");
		label.setLayoutData(new GridData());
		*/
		Button check1 = new Button(composite, SWT.CHECK);
		check1.setText("Convert fields to private final");
		check1.setLayoutData(new GridData());

		return composite;
	}
}
