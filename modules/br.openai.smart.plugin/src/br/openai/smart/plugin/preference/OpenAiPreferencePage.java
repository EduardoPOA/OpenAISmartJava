package br.openai.smart.plugin.preference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import br.openai.smart.plugin.Activator;
import br.openai.smart.plugin.handlers.AskAnything;
import br.openai.smart.plugin.util.EncryptionUtil;
import br.openai.smart.plugin.util.OpenAiConstants;

public class OpenAiPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private String oldApiKey;

	private String oldModel;

	/**
	 * Construtor para OpenAiPreferencePage.
	 * 
	 * @author Eduardo Oliveira
	 */
	public OpenAiPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription(OpenAiConstants.PREFERENCE_PAGE_DESCRIPTION);
		oldApiKey = getPreferenceStore().getString(OpenAiConstants.OPENAI_API_KEY);
		oldModel = getPreferenceStore().getString(OpenAiConstants.OPENAI_MODEL);

	}

	/**
	 * Cria os editores de campo para a página de preferências.
	 *
	 * @author Eduardo Oliveira
	 *
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	public void createFieldEditors() {

		StringFieldEditor apiKeyField = new StringFieldEditor(OpenAiConstants.OPENAI_API_KEY, "OpenAi Secret Key:",
				getFieldEditorParent());
		apiKeyField.getTextControl(getFieldEditorParent()).setEchoChar('*');
		apiKeyField.setEmptyStringAllowed(false);
		apiKeyField.setErrorMessage("API key is required");
		addField(apiKeyField);

		getPreferenceStore().setDefault(OpenAiConstants.OPENAI_LANGUAGE_PORTUGUESE, "portuguese");

		/*
		 * RadioGroupFieldEditor languageField = new
		 * RadioGroupFieldEditor(OpenAiConstants.OPENAI_LANGUAGE, "Language:", 1, new
		 * String[][] { { "Inglês", "english" }, { "Português", "portuguese" }, {
		 * "Espanhol", "spanish" } }, getFieldEditorParent(), true);
		 * addField(languageField);
		 */

		/*
		 * if (languageField.getPreferenceName().equals("Inglês")) {
		 * getPreferenceStore().setDefault(OpenAiConstants.OPENAI_LANGUAGE, "english");
		 * } else if (languageField.getPreferenceName().equals("Português")) {
		 * getPreferenceStore().setDefault(OpenAiConstants.OPENAI_LANGUAGE,
		 * "portuguese"); } else if
		 * (languageField.getPreferenceName().equals("Espanhol")) {
		 * getPreferenceStore().setDefault(OpenAiConstants.OPENAI_LANGUAGE, "spanish");
		 * }
		 */

		/*
		 * String path = "C:\\Users\\eduar\\OneDrive\\Área de Trabalho\\test.txt"; try {
		 * Files.write(Paths.get(path), languageField.getLabelText().getBytes()); }
		 * catch (IOException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */

		IntegerFieldEditor maxTokenField = new IntegerFieldEditor(OpenAiConstants.OPENAI_MAX_TOKEN,
				"Max Token for each request:", getFieldEditorParent());
		maxTokenField.getTextControl(getFieldEditorParent()).setTextLimit(4);
//		maxTokenField.setValidRange(0, 4097);
		addField(maxTokenField);

		StringFieldEditor modelField = new StringFieldEditor(OpenAiConstants.OPENAI_MODEL, "OpenAi Model:",
				getFieldEditorParent());
		addField(modelField);

		GridData gridDataMaxToken = new GridData();
		gridDataMaxToken.widthHint = 50;
		maxTokenField.getTextControl(getFieldEditorParent()).setLayoutData(gridDataMaxToken);

		GridData gridDataModel = new GridData();
		gridDataModel.widthHint = 300;
		modelField.getTextControl(getFieldEditorParent()).setLayoutData(gridDataModel);

		GridData gridDataKey = new GridData();
		gridDataKey.widthHint = 450;
		apiKeyField.getTextControl(getFieldEditorParent()).setLayoutData(gridDataKey);

	}

	/**
	 * Este método é usado para inicializar a página de preferências com valores
	 * padrão
	 *
	 * @author Eduardo Oliveira
	 *
	 * @param arg0 IWorkbench
	 */
	@Override
	public void init(IWorkbench arg0) {
		IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();
		preferenceStore.setDefault(OpenAiConstants.OPENAI_MAX_TOKEN, OpenAiConstants.MAX_TOKEN_DEFAULT);
		preferenceStore.setDefault(OpenAiConstants.OPENAI_MODEL, OpenAiConstants.MODEL_DEFAULT);

	}

	/**
	* Este método é usado para executar a ação 'OK' quando o usuário clica em 'OK'
	* botão na página de preferências. Ele criptografa a chave da API antes de armazená-la.
	*
	*@author Eduardo Oliveira
	*
	* @return booleano
	*/
	@Override
	public boolean performOk() {
		boolean result = super.performOk();
		String apiKey = getPreferenceStore().getString(OpenAiConstants.OPENAI_API_KEY);
		String maxToken = getPreferenceStore().getString(OpenAiConstants.OPENAI_MAX_TOKEN);
		String model = getPreferenceStore().getString(OpenAiConstants.OPENAI_MODEL);
		if (!apiKey.isEmpty() && !apiKey.equals(oldApiKey)) {
			try {
				apiKey = EncryptionUtil.encrypt(apiKey);
				getPreferenceStore().setValue(OpenAiConstants.OPENAI_API_KEY, apiKey);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		getPreferenceStore().setValue(OpenAiConstants.OPENAI_MAX_TOKEN, maxToken);
		if (!model.equals(oldModel) && !model.equals(OpenAiConstants.MODEL_DEFAULT)) {
			try {
				if (!AskAnything.isValidModel(model, EncryptionUtil.decrypt(apiKey))) {
					MessageDialog.openError(getShell(), "Error", "Invalid model. Please enter a valid Modelo name.");
					getPreferenceStore().setToDefault(OpenAiConstants.OPENAI_MODEL);
					return false;
				} else {
					getPreferenceStore().setValue(OpenAiConstants.OPENAI_MODEL, model);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return result;
	}

	/**
	* O método performApply é chamado quando o usuário pressiona o botão Aplicar no
	* a página de preferências. Este método salva as preferências atuais no arquivo
	*loja de preferência.
	*
	*@author Eduardo Oliveira
	*/
	@Override
	protected void performApply() {
		super.performApply();
		String apiKey = getPreferenceStore().getString(OpenAiConstants.OPENAI_API_KEY);
		if (!apiKey.isEmpty()) {
			try {
				apiKey = EncryptionUtil.encrypt(apiKey);
				getPreferenceStore().setValue(OpenAiConstants.OPENAI_API_KEY, apiKey);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	* O método performDefaults é chamado quando o usuário pressiona o botão Defaults
	* botão na página de preferências. Este método define os valores padrão para o
	*preferências.
	*
	*@author Eduardo Oliveira
	*/
	@Override
	protected void performDefaults() {
		super.performDefaults();
		getPreferenceStore().setValue(OpenAiConstants.OPENAI_API_KEY, "");
	}
}