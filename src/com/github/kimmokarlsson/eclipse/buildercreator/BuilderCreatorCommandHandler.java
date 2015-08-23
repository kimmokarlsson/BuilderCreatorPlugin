package com.github.kimmokarlsson.eclipse.buildercreator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;

public class BuilderCreatorCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IEditorInput input = HandlerUtil.getActiveEditorInput(event);
		IWorkingCopyManager wcopy = JavaUI.getWorkingCopyManager();
		try {
			wcopy.connect(input);

			ICompilationUnit cu = wcopy.getWorkingCopy(input);
			modify(cu);
		}
		catch (Exception e) {
			ErrorLog.info("Unable to find java source", e);
			return null;
		}
		finally {
			wcopy.disconnect(input);
		}
		return null;
	}

	private void modify(ICompilationUnit cu) throws JavaModelException {

		CreateBuilderDialog dialog = new CreateBuilderDialog();
		if (dialog.show(cu) == Dialog.OK) {
			
			List<IField> fields = dialog.getFields();
			if (fields == null || fields.size() == 0) {
				return;
			}
			IField field = dialog.getLastField();
			if (field == null) {
				return;
			}
			IBuffer buffer = cu.getBuffer();
			
			if (dialog.getSettings().isConvertFieldsFinal()) {
				for (IField f : fields) {
					convertModifiers(f, cu);
				}
			}
			
			String generatedCode = BuilderCodeGenerator.generate(BuilderCodeGenerator.findFirstClassName(cu), fields, dialog.getSettings());
			int position = field.getSourceRange().getOffset() + field.getSourceRange().getLength();
			buffer.replace(position, 0, generatedCode);
			
			try {
				cu.reconcile(ICompilationUnit.NO_AST, false, null, null);
			} catch (JavaModelException e) {
				ErrorLog.info("Unable to compile java source", e);
			}

			String sourceCode = buffer.getContents();
			Map<String,String> options = new HashMap<>();
			options.put(JavaCore.COMPILER_SOURCE, "1.8");
			options.put(JavaCore.COMPILER_COMPLIANCE, "1.8");
			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.8");
			TextEdit text = ToolFactory.createCodeFormatter(options).format(CodeFormatter.K_COMPILATION_UNIT, sourceCode, 0, sourceCode.length(), 0, null);
			try {
				cu.applyTextEdit(text, null);
				cu.reconcile(ICompilationUnit.NO_AST, false, null, null);
			} catch (JavaModelException e) {
				ErrorLog.info("Unable to compile java source", e);
			}
		}
	}

	private void convertModifiers(IField field, ICompilationUnit cu) throws IndexOutOfBoundsException, JavaModelException {
		IBuffer buffer = cu.getBuffer();
		
		String existings = "";
		String additions = "";
		int flags = field.getFlags();
		if (!Flags.isPrivate(flags)) {
			additions = "private final";
		}
		else {
			existings = "private";
		}
		if (!Flags.isFinal(flags)) {
			additions = "private final";
		}
		else {
			existings = "final";
		}
		String removals = "";
		if (Flags.isProtected(flags)) {
			removals = "protected";
		}
		else if (Flags.isPublic(flags)) {
			removals = "public";
		}
		if (additions.length() > 0 && removals.length() > 0) {
			String original = buffer.getText(field.getSourceRange().getOffset(), field.getSourceRange().getLength());
			String newDecl = original.replace(removals, additions);
			buffer.replace(field.getSourceRange().getOffset(), field.getSourceRange().getLength(), newDecl);
		}
		else if (additions.length() > 0 && existings.length() > 0) {
			String original = buffer.getText(field.getSourceRange().getOffset(), field.getSourceRange().getLength());
			String newDecl = original.replace(existings, additions);
			buffer.replace(field.getSourceRange().getOffset(), field.getSourceRange().getLength(), newDecl);
		}
		else if (additions.length() > 0) {
			buffer.replace(field.getSourceRange().getOffset(), 0, additions + ' ');
		}
		
		try {
			cu.reconcile(ICompilationUnit.NO_AST, false, null, null);
		} catch (JavaModelException e) {
			ErrorLog.info("Unable to compile java source", e);
		}
	}
}