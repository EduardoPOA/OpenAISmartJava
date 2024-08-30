package br.openai.smart.plugin.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;

import br.openai.smart.plugin.util.EncryptionUtil;
import br.openai.smart.plugin.util.OpenAiConstants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FindBug extends AbstractHandler {

	private OpenAiService openAiService;
	
	@Override
	public Object execute(ExecutionEvent event) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

		String apiKey = null;
		try {
			apiKey = getStoredApiKey(window);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (apiKey == null || apiKey.isEmpty()) {
			openPreferencePage(window);
			try {
				apiKey = getStoredApiKey(window);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (apiKey != null) {
			openAiService = new OpenAiService(apiKey, 90);
		} else {
			MessageDialog.openError(window.getShell(), OpenAiConstants.ERROR,
					"Insira a chave secreta OpenAi para gerar código.");
			return null;
		}

		List<String> selectedTexts = getSelectedTexts(window);

		if (selectedTexts.isEmpty()) {
			MessageDialog.openError(window.getShell(), OpenAiConstants.ERROR,
					"Selecione um texto antes de gerar código com OpenAI.");
			return null;
		}

		ProgressMonitorDialog pmd = new ProgressMonitorDialog(window.getShell());
		pmd.open();
		pmd.getProgressMonitor().beginTask("<<Aguarde >> Gerando código com OpenAi...", IProgressMonitor.UNKNOWN);

		String generatedCode = "";
		for (String selectedText : selectedTexts) {
			String result = null;
			try {
				result = generateCodeWithOpenAI(selectedText.trim(), window, pmd, apiKey);
			} catch (Exception e) {
				e.printStackTrace();
			}
			generatedCode += result;
		}
		if (!generatedCode.equals("null") && !generatedCode.equals("")) {
			insertCodeIntoEditor(generatedCode, window);
		}

		pmd.close();
		return null;
	}

	private String generateCodeWithOpenAI(String selectedText, IWorkbenchWindow window, ProgressMonitorDialog pmd,
			String apiKey) throws Exception {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(OpenAiConstants.PLUGIN_ID);

		String model = getModel(window, pmd, apiKey, preferences);
		String maxToken = getMaxToken(preferences);
		String language = getLanguage(window, preferences);
		selectedText = selectedText.replaceAll("\\s+","");
		String prompt = findBugs(selectedText, language, window);

		CompletionRequest completionRequest = CompletionRequest.builder().prompt(prompt).model(model).echo(false)
				.maxTokens(Integer.parseInt(maxToken)).build();
		try {
			String result = openAiService.createCompletion(completionRequest).getChoices().iterator().next().getText()
					.trim();
			return result;
		} catch (Exception e) {
			handleOpenAiError(window, pmd, e);
		}
		return null;
	}

	private String findBugs(String selectedText, String language, IWorkbenchWindow window) throws Exception {

		if (language.equals(OpenAiConstants.LANGUAGE_DEFAULT)) {
			return OpenAiConstants.FIND_BUGS_PORTUGUESE.concat(selectedText);
		} else {
			MessageDialog.openError(window.getShell(), OpenAiConstants.ERROR, "Não foi possível gerar javadoc");
			throw new Exception();
		}
	}

	private List<String> getSelectedTexts(IWorkbenchWindow window) {
		List<String> selectedTexts = new ArrayList<String>();
		IEditorPart editor = window.getActivePage().getActiveEditor();
		if (editor instanceof ITextEditor) {
			ITextEditor textEditor = (ITextEditor) editor;
			IDocumentProvider documentProvider = textEditor.getDocumentProvider();
			IDocument document = documentProvider.getDocument(textEditor.getEditorInput());
			ISelection selection = textEditor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection) {
				ITextSelection textSelection = (ITextSelection) selection;
				int startLine = textSelection.getStartLine();
				int endLine = textSelection.getEndLine();
				StringBuilder sb = new StringBuilder();
				boolean inJavadoc = false;
				for (int i = startLine; i <= endLine; i++) {
					try {
						String lineText = document.get(document.getLineOffset(i), document.getLineLength(i)).trim();
						if (lineText.startsWith("//")) {
							if (sb.length() > 0) {
								selectedTexts.add(sb.toString());
								sb.setLength(0);
							}
							lineText = lineText.substring(2);
							selectedTexts.add(lineText);
						} else if (lineText.startsWith("/**")) {
							inJavadoc = true;
							sb.append(lineText);
						} else if (inJavadoc) {
							sb.append(lineText);
							if (lineText.endsWith("*/")) {
								selectedTexts.add(sb.toString());
								sb.setLength(0);
								inJavadoc = false;
							} else if (i < endLine) {
								sb.append(System.lineSeparator());
							}
						} else if (!lineText.isBlank()) {
							if (selectedTexts.size() > 0) {
								selectedTexts.set(selectedTexts.size() - 1,
										selectedTexts.get(selectedTexts.size() - 1) + lineText);
							} else {
								selectedTexts.add(lineText);
							}
						}
					} catch (org.eclipse.jface.text.BadLocationException e) {
						e.printStackTrace();
					}
				}
				if (sb.length() > 0) {
					selectedTexts.add(sb.toString());
				}
			}
		}
		return selectedTexts;
	}

	private void insertCodeIntoEditor(String code, IWorkbenchWindow window) {
		IEditorPart editor = window.getActivePage().getActiveEditor();
		if (editor instanceof ITextEditor) {
			ITextEditor textEditor = (ITextEditor) editor;
			IDocumentProvider documentProvider = textEditor.getDocumentProvider();
			IDocument document = documentProvider.getDocument(textEditor.getEditorInput());
			ISelection selection = textEditor.getSelectionProvider().getSelection();
			if (selection instanceof ITextSelection) {
				ITextSelection textSelection = (ITextSelection) selection;
				int offset = textSelection.getOffset();
				int length = textSelection.getLength();
				try {
					document.replace(offset, length, code);
				} catch (org.eclipse.jface.text.BadLocationException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void handleOpenAiError(IWorkbenchWindow window, ProgressMonitorDialog pmd, Exception e) {
		pmd.close();
		if (e.getMessage().contains("401")) {
			MessageDialog.openError(window.getShell(), OpenAiConstants.ERROR + " 401 - Forbidden", OpenAiConstants.ERROR
					+ " com conexão OpenAi >>>> Tente executar novamente."
					+ ".\nPor favor, verifique sua chave secreta OpenAi.\nVocê pode alterar sua chave secreta OpenAi nas preferências do Eclipse OpenAi.");
		} else {
			MessageDialog.openError(window.getShell(), OpenAiConstants.ERROR,
					OpenAiConstants.ERROR + " com conexão OpenAi >>>> Tente executar novamente.");
		}
		e.printStackTrace();
	}

	private String getMaxToken(IEclipsePreferences preferences) {
		String maxToken = preferences.get(OpenAiConstants.OPENAI_MAX_TOKEN, null);
		if (maxToken == null || maxToken.isEmpty() || maxToken.isBlank()) {
			maxToken = Integer.toString(OpenAiConstants.MAX_TOKEN_DEFAULT);
		}
		return maxToken;
	}

	private String getModel(IWorkbenchWindow window, ProgressMonitorDialog pmd, String apiKey,
			IEclipsePreferences preferences) throws IOException, Exception {
		String model = preferences.get(OpenAiConstants.OPENAI_MODEL, null);
		if (model == null || model.isEmpty() || model.isBlank()) {
			model = OpenAiConstants.MODEL_DEFAULT;
		} else {
			if (!isValidModel(model, apiKey)) {
				pmd.close();
				MessageDialog.openError(window.getShell(), OpenAiConstants.ERROR, "Modelo ".concat(model)
						.concat(" não é válido.\nPor favor, altere o nome do seu modelo ou restaure o padrão."));
				throw new Exception("Modelo ".concat(model).concat(" não é válido."));
			}
		}
		return model;
	}

	private String getLanguage(IWorkbenchWindow window, IEclipsePreferences preferences) throws Exception {
		String language = preferences.get(OpenAiConstants.LANGUAGE_DEFAULT, null);
		if (language == null || language.isEmpty() || language.isBlank()) {
			return OpenAiConstants.LANGUAGE_DEFAULT;
		}
		return language;

	}

	public static Boolean isValidModel(String model, String apiKey) throws Exception {
		OkHttpClient client = new OkHttpClient().newBuilder().build();
		Request request = new Request.Builder().url(OpenAiConstants.GET_OPENAI_MODELS_URL.concat(model))
				.method(OpenAiConstants.GET, null)
				.addHeader(OpenAiConstants.AUTHORIZATION, OpenAiConstants.BEARER.concat(apiKey)).build();
		Response response = client.newCall(request).execute();
		if (response.code() == 200)
			return true;
		if (response.code() == 404)
			return false;
		throw new Exception("Error: " + response.code());
	}

	private void openPreferencePage(IWorkbenchWindow window) {
		PreferenceDialog preferenceDialog = PreferencesUtil.createPreferenceDialogOn(window.getShell(),
				OpenAiConstants.OPENAI_PREFERENCE_PAGE_PATH, null, null);
		preferenceDialog.open();
	}

	private String getStoredApiKey(IWorkbenchWindow window) throws Exception {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(OpenAiConstants.PLUGIN_ID);
		return EncryptionUtil.decrypt(preferences.get(OpenAiConstants.OPENAI_API_KEY, null));
	}
}
