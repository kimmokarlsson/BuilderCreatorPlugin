package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * Renaming Builder class field with parent class field.
 */
public class BuilderFieldRenameParticipant extends RenameParticipant {

	private ICompilationUnit unit;
	private String oldName;
	private IField field;
	private IField builderField;
	private IMethod builderConstr;
	private IMethod builderSetter;
	private IMethod parentConstr;

	/**
	 * @return false if could not participate
	 */
	@Override
	protected boolean initialize(Object element) {
		if (element instanceof IField) {
			field = (IField) element;
			oldName = field.getElementName();
			unit = field.getCompilationUnit();
			if (field.getParent() instanceof IType) {
				IType type = (IType) field.getParent();
				try {
					for (IMethod m : type.getMethods()) {
						if (m.getElementName().equals(type.getElementName()) && m.getParameterTypes().length == 1
								&& "QBuilder;".equals(m.getParameterTypes()[0])) {
							parentConstr = m;
						}
					}
					IType builderClass = null;
					for (IType mt : type.getTypes()) {
					    if ("Builder".equals(mt.getElementName())) {
							builderClass = mt;
						}
					}
					if (builderClass != null) {
						for (IField bf : builderClass.getFields()) {
							if (bf.getElementName().equals(field.getElementName())) {
								builderField = bf;
							}
						}
						for (IMethod bm : builderClass.getMethods()) {
							if (bm.getElementName().equals("Builder") && bm.getParameterTypes().length == 1) {
								builderConstr = bm;
							}
							else if (bm.getElementName().equals(field.getElementName()) && bm.getParameterTypes().length == 1) {
								builderSetter = bm;
							}
						}
					}
				} catch (JavaModelException e) {
					ErrorLog.warn("Checking child classes", e);
				}
			}
		}
		return builderField != null && builderSetter != null && builderConstr != null && parentConstr != null;
	}

	@Override
	public String getName() {
		return "Builder Field Renamer";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm,
			CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {

		TextChange change = getTextChange(unit);
		if (change == null) {
			return null;
		}

		String newName = getArguments().getNewName();

		// assignment in setter method in builder class
		{
			int start = builderSetter.getSourceRange().getOffset();
			int nameStart = builderSetter.getSource().indexOf(oldName);
			int index = builderSetter.getSource().indexOf(oldName, nameStart+oldName.length());
			change.addEdit(new ReplaceEdit(start+index, oldName.length(), newName));
		}
		// setter method name in builder class
		{
			ISourceRange nameRange = builderSetter.getNameRange();
			change.addEdit(new ReplaceEdit(nameRange.getOffset(), nameRange.getLength(), newName));
		}
		// assignment in builder class constructor
		{
			int start = builderConstr.getSourceRange().getOffset();
			int index = builderConstr.getSource().indexOf(oldName);
			change.addEdit(new ReplaceEdit(start+index, oldName.length(), newName));
		}
		// field declaration in builder class
		{
			ISourceRange nameRange = builderField.getNameRange();
			change.addEdit(new ReplaceEdit(nameRange.getOffset(), nameRange.getLength(), newName));
		}
		// assignment in parent class constructor
		{
			int start = parentConstr.getSourceRange().getOffset();
			int nameStart = parentConstr.getSource().indexOf(oldName);
			int index = parentConstr.getSource().indexOf(oldName, nameStart+oldName.length());
			change.addEdit(new ReplaceEdit(start+index, oldName.length(), newName));
		}
		return null;
	}
}
