package org.javastack.jhtmleditor;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class LinkDialog extends JDialog {
	private static final long serialVersionUID = 42L;
	private static final int V_SPACE = 5; // TODO: Hardcoded

	private final AtomicBoolean accepted = new AtomicBoolean();
	private final JTextField url;

	public LinkDialog(final Frame parent) {
		// Modal
		super(parent, "Link", true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		//
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		//
		JPanel linkHeaderPanel = makeHeaderPanel("Link");
		JPanel linkPanel = new JPanel();
		linkPanel.setLayout(new BoxLayout(linkPanel, BoxLayout.LINE_AXIS));
		url = new JTextField(20);
		linkPanel.add(url);
		linkHeaderPanel.add(linkPanel);
		panel.add(linkHeaderPanel);
		panel.add(Box.createRigidArea(new Dimension(5, V_SPACE)));
		//
		JPanel buttons = new JPanel() {
			private static final long serialVersionUID = 42L;

			{
				setLayout(new FlowLayout(FlowLayout.CENTER));
				//
				JButton add = new JButton(UIManager.getString("OptionPane.okButtonText"));
				add.addActionListener(e -> {
					accepted.set(false);
					if (!isValidURL(url.getText())) {
						url.putClientProperty("JComponent.outline", "error");
						url.requestFocusInWindow();
						return;
					}
					accepted.set(true);
					dispose();
				});
				add(add);
				//
				JButton cancel = new JButton(UIManager.getString("OptionPane.cancelButtonText"));
				cancel.addActionListener(e -> dispose());
				add(cancel);
				//
				LinkDialog.this.getRootPane().setDefaultButton(add); // Default on INTRO
			}
		};
		panel.add(buttons);
		JPanel padded = new JPanel();
		padded.add(panel);
		getContentPane().add(padded);
		//
		// Prepare window.
		pack();
		setLocationRelativeTo(null);
		setResizable(false);
	}

	public void setLink(final String url) {
		this.url.setText(url);
	}

	public String getLink() {
		return this.url.getText();
	}

	public boolean isAccepted() {
		return this.accepted.get();
	}

	protected boolean isValidURL(final String url) {
		if (url.isEmpty())
			return false;
		if (url.startsWith("https://") || url.startsWith("http://") //
				|| url.startsWith("ftp://") || url.startsWith("ftps://") //
				|| url.startsWith("mailto:")) {
			try {
				new URL(url);
				return true;
			} catch (MalformedURLException e) {
			}
		}
		return false;
	}

	public void init() {
		// Display the window.
		setVisible(true);
	}

	private JPanel makeHeaderPanel(final String text) {
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		return panel;
	}
}