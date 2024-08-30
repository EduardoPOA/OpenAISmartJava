package br.openai.smart.plugin.util;

public class OpenAiConstants {

	public static final String PLUGIN_ID = "br.openai.smart.plugin";

	// preference key
	public static final String OPENAI_API_KEY = "openai_api_key";
	public static final String OPENAI_MAX_TOKEN = "max_token";
	public static final String OPENAI_MODEL = "openai_model";
	public static final String OPENAI_LANGUAGE = "openai_language";
	public static final String OPENAI_LANGUAGE_ENGLISH = "english";
	public static final String OPENAI_LANGUAGE_PORTUGUESE = "portuguese";
	public static final String OPENAI_LANGUAGE_SPANISH = "spanish";
	public static final String MODEL_DEFAULT = "text-davinci-003";
	public static final String LANGUAGE_DEFAULT = "Java";
	public static final int MAX_TOKEN_DEFAULT = 2048;

	public static final String MAX_TOKEN_DESCRIPTION = "Enter the maximum number of tokens allowed for one request:";
	public static final String API_KEY_LABEL = "API Key:";
	public static final String MAX_TOKEN_LABEL = "Max Token:";
	public static final String PREFERENCE_PAGE_DESCRIPTION = "OpenAISmartTest General Preferences:";
	public static final String INSERT_KEY_DIALOG_DESCRIPTION = "Enter your OpenAI API secret key:";
	public static final String OPENAI_PREFERENCE_PAGE_PATH = "openaiplugin.preference.openai";
	public static final byte[] KEY = "MySuperSecretKey".getBytes();
	public static final String ALGORITHM = "AES";
	public static final String JAVADOC_CREATE_PROMPT = "Rewrite the method and just comment with javadadoc format and refactor: ";
	public static final String JAVADOC_CREATE_PROMPT_PORTUGUESE = "Rewrite the method and just comment translated into Portuguese with javadadoc format and refactor: ";
	public static final String JAVADOC_CREATE_PROMPT_SPANISH = "ORewrite the method and just comment translated into Spanish with javadadoc format and refactor: ";
	public static final String DESCRIPTION_STATIC_PROMPT = "Just write me the code of a method in %s, which: ";
	public static final String DESCRIPTION_STATIC_PROMPT_JAVADOC = "Code it by use cases. Write only the code, not the explanation: ";
	public static final String JAVA_REFACTOR_FUNCTION_PROMPT = "Can you refactor this function?: ";
	public static final String EXPLANATION = "Explain and rewrite the method, and write only in single comments format like: ";
	public static final String EXPLANATION_PORTUGUESE = "Explain and rewrite the method, and write only comments in Portuguese in single comments format like: ";
	public static final String FIND_BUGS = "Find bugs and rewrite the method, and write only in single comments format like: ";
	public static final String FIND_BUGS_PORTUGUESE = "Find bug and rewrite the method, and write only comments in Portuguese in single comments format like: ";
	public static final String CREATE_UNIT_TESTS = "Rewrite the method refactored, and below create unit tests with JUnit: ";
	public static final String ERROR = "Error";
	public static final String GET = "GET";
	public static final String AUTHORIZATION = "Authorization";
	public static final String BEARER = "Bearer ";
	public static final String GET_OPENAI_MODELS_URL = "https://api.openai.com/v1/models/";

}