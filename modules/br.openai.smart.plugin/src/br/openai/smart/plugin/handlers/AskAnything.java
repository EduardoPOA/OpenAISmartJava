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
import br.openai.smart.plugin.util.JavaFunctionRecognizer;
import br.openai.smart.plugin.util.OpenAiConstants;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AskAnything extends AbstractHandler {

	private OpenAiService openAiService;

	/**
	 * Execute o método da classe AskAnything, lida com o código processo de geração
	 *
	 * @author Eduardo Oliveira>
	 *
	 * @param event ExecutionEvent
	 * @return Object
	 */
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

	/**
	 * Insere o código fornecido no editor atualmente ativo no momento seleção.
	 * 
	 * @author Eduardo Oliveira
	 * 
	 * @param code   Código para inserir
	 * @param window A janela do ambiente de trabalho atual
	 */
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

	/**
	 * Retorna uma lista dos textos selecionados no editor atualmente ativo.
	 * 
	 * @author Eduardo Oliveira
	 * 
	 * @param window A janela do ambiente de trabalho atual
	 * @return Uma lista dos textos selecionados
	 */
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

	/**
	 * Abra a página de preferências para inserir a chave secreta OpenAI
	 *
	 * @author Eduardo Oliveira
	 *
	 * @param window IWorkbenchWindow
	 */
	private void openPreferencePage(IWorkbenchWindow window) {
		PreferenceDialog preferenceDialog = PreferencesUtil.createPreferenceDialogOn(window.getShell(),
				OpenAiConstants.OPENAI_PREFERENCE_PAGE_PATH, null, null);
		preferenceDialog.open();
	}

	/**
	 * Obtenha a chave de API armazenada nas preferências
	 *
	 * @author Eduardo Oliveira
	 *
	 * @param window IWorkbenchWindow
	 * @return String
	 * @throws Exception
	 */
	private String getStoredApiKey(IWorkbenchWindow window) throws Exception {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(OpenAiConstants.PLUGIN_ID);
		return EncryptionUtil.decrypt(preferences.get(OpenAiConstants.OPENAI_API_KEY, null));
	}

	/**
	 * Generates code using OpenAI API
	 *
	 * @author Eduardo Oliveira
	 *
	 * @param selectedText o texto selecionado
	 * @param window       a janela do ambiente de trabalho ativo
	 * @param pmd          a caixa de diálogo do monitor de progresso
	 * @return o código gerado
	 * @throws Exception
	 */
	private String generateCodeWithOpenAI(String selectedText, IWorkbenchWindow window, ProgressMonitorDialog pmd,
			String apiKey) throws Exception {
		IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(OpenAiConstants.PLUGIN_ID);

		String model = getModel(window, pmd, apiKey, preferences);
		String maxToken = getMaxToken(preferences);
		String language = getLanguage(window, preferences);
		selectedText = selectedText.replaceAll("\\s+", "");
		String prompt = getPrompt(selectedText, language, window);

		CompletionRequest completionRequest = CompletionRequest.builder().prompt(prompt).model(model).echo(false)
				.maxTokens(Integer.parseInt(maxToken)).build();
		try {
			String result = openAiService.createCompletion(completionRequest).getChoices().iterator().next().getText()
					.trim();
			if (isJavaDocComment(selectedText)) {
				return selectedText + "\n" + result;
			}
			return result;
		} catch (Exception e) {
			handleOpenAiError(window, pmd, e);
		}
		return null;
	}

	/**
	 * Este método trata o erro gerado quando o serviço OpenAI é não é possível
	 * gerar o código. Mostra uma mensagem de erro ao usuário com o mensagem de erro
	 * específica e o código de status.
	 * 
	 * @author Eduardo Oliveira
	 * 
	 * @param window A janela do ambiente de trabalho ativo
	 * @param e      A exceção que foi lançada
	 */
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

	/**
	 * Este método recupera o prompt do serviço OpenAI com base no selecionado texto
	 * e as configurações do plugin.
	 * 
	 * @author Eduardo Oliveira
	 * 
	 * @param selectedText O texto que foi selecionado pelo usuário
	 * @return O prompt que será usado para o OpenAI
	 * @throws Exception
	 */
	public String getPrompt(String selectedText, String language, IWorkbenchWindow window) throws Exception {

		if (language.equals(OpenAiConstants.LANGUAGE_DEFAULT)) {
			if (!JavaFunctionRecognizer.isFunction(selectedText)) {
				return String.format(OpenAiConstants.DESCRIPTION_STATIC_PROMPT_JAVADOC, language).concat(selectedText);
			} else {
				return OpenAiConstants.JAVA_REFACTOR_FUNCTION_PROMPT.concat(selectedText);
			}
		} else if (!language.isBlank() && !language.isEmpty()) {
			return String.format(OpenAiConstants.DESCRIPTION_STATIC_PROMPT, language).concat(selectedText);
		}
		return null;
	}

	public static String insertJavadoc(String selectedText, String language, IWorkbenchWindow window) throws Exception {

		if (language.equals(OpenAiConstants.OPENAI_LANGUAGE_PORTUGUESE)) {
			return OpenAiConstants.JAVADOC_CREATE_PROMPT_PORTUGUESE.concat(selectedText);
		} else {
			MessageDialog.openError(window.getShell(), OpenAiConstants.ERROR, "Não foi possível gerar javadoc");
			throw new Exception();
		}
	}

	/**
	 * Retorna o número máximo de tokens (palavras) que a API OpenAI deve gerar. Se
	 * o número máximo de tokens não estiver definido nas preferências, o o valor
	 * padrão é retornado.
	 * 
	 * @author Eduardo Oliveira
	 * 
	 * @param preferências As preferências do plugin
	 * @return O número máximo de tokens (palavras) que a API OpenAI deve gerar
	 */
	private String getMaxToken(IEclipsePreferences preferences) {
		String maxToken = preferences.get(OpenAiConstants.OPENAI_MAX_TOKEN, null);
		if (maxToken == null || maxToken.isEmpty() || maxToken.isBlank()) {
			maxToken = Integer.toString(OpenAiConstants.MAX_TOKEN_DEFAULT);
		}
		return maxToken;
	}

	/**
	 * Retorna o nome do modelo que deve ser utilizado para geração de código com
	 * OpenAI. Se o modelo não estiver definido nas preferências, o valor padrão
	 * será devolvida. Se o modelo estiver definido, mas não for válido, uma
	 * mensagem de erro será exibida e uma exceção é lançada.
	 * 
	 * @author Eduardo Oliveira
	 * 
	 * @param window       A janela do ambiente de trabalho ativo
	 * @param pmd          A caixa de diálogo do monitor de progresso
	 * @param apiKey       A chave API para OpenAI
	 * @param preferências As preferências do plugin
	 * @return O nome do modelo a ser usado para geração de código
	 * @throws IOException Se ocorrer um erro durante a comunicação com o OpenAI API
	 * @throws Exceção     Se o modelo não for válido
	 */
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

	/**
	 * Retorna o nome da linguagem que deve ser utilizada para geração de código com
	 * OpenAI. Se o idioma não estiver definido nas preferências, o valor padrão
	 * será retornado e uma mensagem de erro é exibida e uma exceção é lançada.
	 *
	 * @param janela
	 * @param preferências
	 * @retornar
	 * @throws Exceção
	 */
	private String getLanguage(IWorkbenchWindow window, IEclipsePreferences preferences) throws Exception {
		String language = preferences.get(OpenAiConstants.OPENAI_LANGUAGE, null);
		if (language == null || language.isEmpty() || language.isBlank()) {
			return OpenAiConstants.LANGUAGE_DEFAULT;
		}
		return language;

	}

	/**
	 * Este método verifica se a string de entrada é um comentário JavaDoc.
	 *
	 * @author Eduardo Oliveira
	 *
	 * @param insira a string de entrada para verificar
	 * @return true se a string de entrada for um comentário JavaDoc, false caso
	 *         contrário
	 */

	public static boolean isJavaDocComment(String input) {
		if (input.startsWith("/**") && input.endsWith("*/")) {
			if (input.matches("(?s)/\\*\\*.*\\n\\s*\\*\\s*(.+\\n)*.*\\*/.*"))
				return true;
		}
		return false;
	}

	/**
	 *
	 * Este método é usado para verificar se o modelo OpenAI fornecido é válido ou
	 * não. Isto faz uma solicitação GET para a API OpenAI e retorna verdadeiro se o
	 * código de resposta é 200.
	 *
	 * @author Eduardo Oliveira
	 *
	 * @param Modelo  O nome do modelo a ser verificado
	 * @param apiKey A chave da API OpenAI a ser usada para a solicitação
	 * @return booleano indicando se o modelo é válido ou não
	 * @throws Exceção
	 */
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
}