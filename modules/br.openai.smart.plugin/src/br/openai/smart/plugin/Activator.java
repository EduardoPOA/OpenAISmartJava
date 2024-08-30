package br.openai.smart.plugin;

import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import br.openai.smart.plugin.util.OpenAiConstants;

/**
 * A classe ativadora controla o ciclo de vida do plug-in
 */
public class Activator extends AbstractUIPlugin implements IStartup {

	// The plug-in ID
	public static final String PLUGIN_ID = OpenAiConstants.PLUGIN_ID; // $NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	public void earlyStartup() {
	}

	/**
	 * O construtor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

}
