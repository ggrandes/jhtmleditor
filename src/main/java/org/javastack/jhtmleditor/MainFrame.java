package org.javastack.jhtmleditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLWriter;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.StyleSheet;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.fit.net.DataURLHandler;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignU;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;
import org.owasp.html.CssSchema;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme;

public class MainFrame {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final boolean useDark = true;
	private static final boolean enableBrokenFeatures = false;
	private static final PolicyFactory HTML_POLICY_DEFINITION_BASIC = new HtmlPolicyBuilder()
			.allowUrlProtocols("http", "https", "data", "mailto") //
			.allowAttributes("id").globally() //
			.allowAttributes("class").globally() //
			.allowStyling(CssSchema.DEFAULT) //
			.allowAttributes("src").onElements("img") //
			.allowAttributes("alt").onElements("img") //
			.allowAttributes("height", "width").onElements("img") //
			.allowAttributes("href").onElements("a") //
			.allowAttributes("color").onElements("font") //
			.allowElements("p", "div", "b", "i", "u", "strike", "font", "a", "img") //
			.toFactory();

	private JFrame frame;

	/**
	 * Launch the application.
	 * 
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				try {
					log.info("Load decorators...");
					UIManager.put("TitlePane.menuBarEmbedded", false);
					if (useDark) {
						FlatDarculaLaf.setup();
					} else {
						FlatLightFlatIJTheme.setup();
					}
					FlatLaf.updateUI();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					log.info("Open window...");
					MainFrame window = new MainFrame();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainFrame() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	protected void initialize() {
		frame = new JFrame("Simple HTML Text Editor");
		frame.setMinimumSize(new Dimension(763, 300));
		frame.setLocation(100, 100);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final JTextPane editorPane = new JTextPane();
		editorPane.setContentType("text/html");
		editorPane.setBackground(Color.WHITE);
		final JScrollPane editorScrollPane = new JScrollPane();
		editorScrollPane.setViewportView(editorPane);
		final CustomHTMLEditorKit kit = new CustomHTMLEditorKit();
		editorPane.setEditorKit(kit);
		fixEditorCssRules(kit);
		kit.setAutoFormSubmission(false);
		final HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
		doc.setPreservesUnknownTags(false);
		doc.putProperty("IgnoreCharsetDirective", true);
		editorPane.setDocument(doc);
		editorPane.setText(createEmptyDocument());

		// Undo / Redo Feature
		final UndoManager undoManager = new UndoManager();
		doc.addUndoableEditListener(undoManager);

		// Register custom Actions
		final ActionMap editorActionMap = editorPane.getActionMap();
		editorActionMap.put("Undo", new AbstractAction() {
			private static final long serialVersionUID = 42L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					if (undoManager.canUndo()) {
						undoManager.undo();
					}
				} catch (CannotUndoException ex) {
					throw new RuntimeException(ex);
				}
			}
		});
		editorActionMap.put("Redo", new AbstractAction() {
			private static final long serialVersionUID = 42L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					if (undoManager.canRedo()) {
						undoManager.redo();
					}
				} catch (CannotUndoException ex) {
					throw new RuntimeException(ex);
				}
			}
		});
		editorActionMap.put("font-strike", new StrikethroughAction());
		editorActionMap.put("increment-indent", new IndentAction("increment-indent", 20f));
		editorActionMap.put("reduce-indent", new IndentAction("reduce-indent", -20f));
		editorActionMap.put("justified",
				new StyledEditorKit.AlignmentAction("Justify", StyleConstants.ALIGN_JUSTIFIED));
		editorActionMap.put("link", new LinkAction());
		editorActionMap.put("unlink", new UnlinkAction());
		editorActionMap.put("insert-hr", new HTMLEditorKit.InsertHTMLTextAction("insert-hr",
				"<hr size=1 align=left noshade>", HTML.Tag.BODY, HTML.Tag.HR));
		editorActionMap.put("insert-img", new InsertImageAction());

		JToolBar toolBar = new JToolBar();
		frame.getContentPane().add(toolBar, BorderLayout.NORTH);

		// BOLD
		JButton btnBold = new JButton(editorActionMap.get("font-bold"));
		btnBold.setText("B");
		btnBold.setRequestFocusEnabled(false);
		btnBold.setToolTipText("Bold");
		IconHelper.set(MaterialDesignF.FORMAT_BOLD, btnBold);
		mapKey(editorPane, KeyEvent.VK_B, "font-bold");
		toolBar.add(btnBold);

		// ITALIC
		JButton btnItalic = new JButton(editorActionMap.get("font-italic"));
		btnItalic.setText("I");
		btnItalic.setRequestFocusEnabled(false);
		btnItalic.setToolTipText("Italic");
		IconHelper.set(MaterialDesignF.FORMAT_ITALIC, btnItalic);
		mapKey(editorPane, KeyEvent.VK_I, "font-italic");
		toolBar.add(btnItalic);

		// UNDERLINE
		JButton btnUnderline = new JButton(editorActionMap.get("font-underline"));
		btnUnderline.setText("U");
		btnUnderline.setRequestFocusEnabled(false);
		btnUnderline.setToolTipText("Underline");
		{
			final Font fontUnderline = btnUnderline.getFont();
			final Map<TextAttribute, Object> attributes = new HashMap<>(fontUnderline.getAttributes());
			attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
			btnUnderline.setFont(fontUnderline.deriveFont(attributes));
		}
		IconHelper.set(MaterialDesignF.FORMAT_UNDERLINE, btnUnderline);
		mapKey(editorPane, KeyEvent.VK_U, "font-underline");
		toolBar.add(btnUnderline);

		// STRIKE-THROUGH
		JButton btnStrikethrough = new JButton(editorActionMap.get("font-strike"));
		btnStrikethrough.setText("S");
		btnStrikethrough.setRequestFocusEnabled(false);
		btnStrikethrough.setToolTipText("Strikethrough");
		{
			final Font fontStrikethrough = btnStrikethrough.getFont();
			final Map<TextAttribute, Object> attributes = new HashMap<>(fontStrikethrough.getAttributes());
			attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
			btnStrikethrough.setFont(fontStrikethrough.deriveFont(attributes));
		}
		IconHelper.set(MaterialDesignF.FORMAT_STRIKETHROUGH, btnStrikethrough);
		mapKey(editorPane, KeyEvent.VK_S, "font-strike");
		toolBar.add(btnStrikethrough);

		// FONT COLOR
		ColorSelectorButton btnFontColor = new ColorSelectorButton();
		btnFontColor.setRequestFocusEnabled(false);
		btnFontColor.setToolTipText("Font Color");
		IconHelper.setColorHelper(MaterialDesignF.FORMAT_COLOR_TEXT, btnFontColor);
		toolBar.add(btnFontColor);

		toolBar.addSeparator();

		// REALLY BROKEN
		if (enableBrokenFeatures) {
			JButton btnOrderedListItem = new JButton(editorActionMap.get("InsertOrderedListItem"));
			btnOrderedListItem.setText("OL");
			btnOrderedListItem.setRequestFocusEnabled(false);
			btnOrderedListItem.setToolTipText("Ordered List");
			IconHelper.set(MaterialDesignF.FORMAT_LIST_NUMBERED, btnOrderedListItem);
			toolBar.add(btnOrderedListItem);

			JButton btnUnorderedListItem = new JButton(editorActionMap.get("InsertUnorderedListItem"));
			btnUnorderedListItem.setText("UL");
			btnUnorderedListItem.setRequestFocusEnabled(false);
			btnUnorderedListItem.setToolTipText("Unordered List");
			IconHelper.set(MaterialDesignF.FORMAT_LIST_BULLETED, btnUnorderedListItem);
			toolBar.add(btnUnorderedListItem);
		}

		// PARAGRAPH + INDENT
		JButton btnIncreaseIndent = new JButton(editorActionMap.get("increment-indent"));
		btnIncreaseIndent.setText("+Indent");
		btnIncreaseIndent.setRequestFocusEnabled(false);
		btnIncreaseIndent.setToolTipText("Increase Indent");
		IconHelper.set(MaterialDesignF.FORMAT_INDENT_INCREASE, btnIncreaseIndent);
		toolBar.add(btnIncreaseIndent);

		// PARAGRAPH - INDENT
		JButton btnReduceIndent = new JButton(editorActionMap.get("reduce-indent"));
		btnReduceIndent.setText("-Indent");
		btnReduceIndent.setRequestFocusEnabled(false);
		btnReduceIndent.setToolTipText("Reduce Indent");
		IconHelper.set(MaterialDesignF.FORMAT_INDENT_DECREASE, btnReduceIndent);
		toolBar.add(btnReduceIndent);

		toolBar.addSeparator();

		// PARAGRAPH LEFT ALIGN
		JButton btnLeftAlign = new JButton(editorActionMap.get("left-justify"));
		btnLeftAlign.setText("Left");
		btnLeftAlign.setRequestFocusEnabled(false);
		btnLeftAlign.setToolTipText("Left Alignment");
		IconHelper.set(MaterialDesignF.FORMAT_ALIGN_LEFT, btnLeftAlign);
		toolBar.add(btnLeftAlign);

		// PARAGRAPH CENTER ALIGN
		JButton btnCenterAlign = new JButton(editorActionMap.get("center-justify"));
		btnCenterAlign.setText("Center");
		btnCenterAlign.setRequestFocusEnabled(false);
		btnCenterAlign.setToolTipText("Center Alignment");
		IconHelper.set(MaterialDesignF.FORMAT_ALIGN_CENTER, btnCenterAlign);
		toolBar.add(btnCenterAlign);

		// PARAGRAPH RIGHT ALIGN
		JButton btnRightAlign = new JButton(editorActionMap.get("right-justify"));
		btnRightAlign.setText("Right");
		btnRightAlign.setRequestFocusEnabled(false);
		btnRightAlign.setToolTipText("Right Alignment");
		IconHelper.set(MaterialDesignF.FORMAT_ALIGN_RIGHT, btnRightAlign);
		toolBar.add(btnRightAlign);

		// PARAGRAPH JUSTIFIED
		JButton btnJustify = new JButton(editorActionMap.get("justified"));
		btnJustify.setText("Justify");
		btnJustify.setRequestFocusEnabled(false);
		btnJustify.setToolTipText("Justify Alignment");
		IconHelper.set(MaterialDesignF.FORMAT_ALIGN_JUSTIFY, btnJustify);
		toolBar.add(btnJustify);

		toolBar.addSeparator();

		// TEXT HYPERLINK
		JButton btnLink = new JButton(editorActionMap.get("link"));
		btnLink.setText("Link");
		btnLink.setRequestFocusEnabled(false);
		btnLink.setToolTipText("Link");
		IconHelper.set(MaterialDesignL.LINK_VARIANT, btnLink);
		toolBar.add(btnLink);

		// TEXT CLEAR HYPERLINK
		JButton btnUnlink = new JButton(editorActionMap.get("unlink"));
		btnUnlink.setText("Unlink");
		btnUnlink.setRequestFocusEnabled(false);
		btnUnlink.setToolTipText("Unlink");
		IconHelper.set(MaterialDesignL.LINK_VARIANT_OFF, btnUnlink);
		toolBar.add(btnUnlink);

		toolBar.addSeparator();

		// HORIZONTAL RULE
		JButton btnHorizontalRule = new JButton(editorActionMap.get("insert-hr"));
		btnHorizontalRule.setText("\u2015");
		btnHorizontalRule.setRequestFocusEnabled(false);
		btnHorizontalRule.setToolTipText("Horizontal Rule");
		IconHelper.set(MaterialDesignM.MINUS, btnHorizontalRule);
		toolBar.add(btnHorizontalRule);

		// IMAGE
		JButton btnImage = new JButton(editorActionMap.get("insert-img"));
		btnImage.setText("Img");
		btnImage.setRequestFocusEnabled(false);
		btnImage.setToolTipText("Image");
		IconHelper.set(MaterialDesignI.IMAGE, btnImage);
		toolBar.add(btnImage);

		toolBar.addSeparator();

		// UNDO CHANGE
		JButton btnUndo = new JButton(editorActionMap.get("Undo"));
		btnUndo.setText("Undo");
		btnUndo.setRequestFocusEnabled(false);
		btnUndo.setToolTipText("Undo");
		IconHelper.set(MaterialDesignU.UNDO, btnUndo);
		mapKey(editorPane, KeyEvent.VK_Z, "Undo");
		toolBar.add(btnUndo);

		// REDO CHANGE
		JButton btnRedo = new JButton(editorActionMap.get("Redo"));
		btnRedo.setText("Redo");
		btnRedo.setRequestFocusEnabled(false);
		btnRedo.setToolTipText("Redo");
		IconHelper.set(MaterialDesignR.REDO, btnRedo);
		mapKey(editorPane, KeyEvent.VK_Y, "Redo");
		toolBar.add(btnRedo);

		toolBar.addSeparator();

		// SAVE FILE
		JButton btnSave = new JButton("Save");
		btnSave.setText("Save");
		btnSave.setRequestFocusEnabled(false);
		btnSave.setToolTipText("Save");
		btnSave.addActionListener(a -> writeFile(editorPane));
		IconHelper.set(MaterialDesignF.FLOPPY, btnSave);
		toolBar.add(btnSave);

		// LOAD FILE
		JButton btnLoad = new JButton("Load");
		btnLoad.setText("Load");
		btnLoad.setRequestFocusEnabled(false);
		btnLoad.setToolTipText("Load");
		btnLoad.addActionListener(a -> loadFile(editorPane));
		IconHelper.set(MaterialDesignF.FOLDER_OPEN, btnLoad);
		toolBar.add(btnLoad);

		// DEBUG JEDITOR MODEL
		if (enableBrokenFeatures) {
			JButton btnDump = new JButton(editorActionMap.get("dump-model"));
			btnDump.setText("Dump");
			btnDump.setRequestFocusEnabled(false);
			btnDump.setToolTipText("Dump");
			IconHelper.set(MaterialDesignL.LADYBUG, btnDump);
			toolBar.add(btnDump);
		}

		// Focus gain/lost enable/disable butons
		editorPane.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				for (final Component comp : toolBar.getComponents()) {
					comp.setEnabled(true);
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				for (final Component comp : toolBar.getComponents()) {
					comp.setEnabled(false);
				}
			}
		});
		afterLoad(editorPane);

		frame.getContentPane().add(editorScrollPane, BorderLayout.CENTER);
	}

	protected void mapKey(final JTextPane editor, final int keyCode, final String actionMapKey) {
		final InputMap im = editor.getInputMap(JComponent.WHEN_FOCUSED);
		final int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		im.put(KeyStroke.getKeyStroke(keyCode, mask), actionMapKey);
	}

	protected void afterLoad(final JTextPane editor) {
		// Goto first Paragraph
		final StyledDocument doc = editor.getStyledDocument();
		final int len = doc.getLength();
		for (int i = 0; i <= len; i++) {
			final Element elem = doc.getParagraphElement(i);
			final AttributeSet attr = elem.getAttributes();
			final Object o = attr.getAttribute(StyleConstants.NameAttribute);
			if (o == HTML.Tag.P) {
				editor.setCaretPosition(elem.getStartOffset());
				break;
			}
			i = elem.getEndOffset() - 1; // fast-forward
		}
		editor.requestFocus();
	}

	protected void fixEditorCssRules(final HTMLEditorKit kit) {
		// TODO: Fix font scaling: http://kynosarges.org/GuiDpiScaling.html
		// https://stackoverflow.com/questions/17551537/how-to-fit-font-into-pixel-size-in-java-how-to-convert-pixels-to-points
		// https://stackoverflow.com/questions/15659044/how-to-set-the-dpi-of-java-swing-apps-on-windows-linux
		final StyleSheet css = kit.getStyleSheet();
		css.addRule("body, p, li { font-size: 1.1em; }");
	}

	protected String createEmptyDocument() {
		return "<html><head>" //
				+ "<style type=\"text/css\">" //
				+ "body { color: black; background-color: white; font-family: \"Verdana\"; font-size: 10pt; font-weight: normal; font-style: normal; }" //
				+ "p { margin-top: 2px; margin-bottom: 2px; }" //
				+ "hr { border-top: 1px solid gray; }" //
				+ "ol, ul { margin-left: 40px; margin-top: 2px; }" //
				+ "</style></head><body>" //
				+ "<p></p>" //
				+ "</body></html>";
	}

	protected void loadFile(final JTextPane editor) {
		if (editor == null) {
			return;
		}
		final HTMLDocument doc = (HTMLDocument) editor.getDocument();
		final HTMLEditorKit kit = (HTMLEditorKit) editor.getEditorKit();
		final JFileChooser chooser = new JFileChooser();
		chooser.setRequestFocusEnabled(false);
		chooser.setCurrentDirectory(new File("."));
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileFilter(new FileNameExtensionFilter("HTML files", //
				new String[] {
						"html", "htm"
				}));
		final int returnVal = chooser.showOpenDialog(null);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return;
		}
		final File file = chooser.getSelectedFile();
		try {
			// final String text = sanitizeHTML(Files.readString(file.toPath()));
			editor.setText("");
			// kit.read(new StringReader(text), doc, 0);
			kit.read(new FileReader(file), doc, 0);
			afterLoad(editor);
		} catch (BadLocationException | IOException e) {
			throw new RuntimeException(e);
		}

	}

	protected void writeFile(final JTextPane editor) {
		if (editor == null) {
			return;
		}
		final JFileChooser chooser = new JFileChooser();
		chooser.setRequestFocusEnabled(false);
		// Path relativize(Path other)
		chooser.setCurrentDirectory(new File("."));
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileFilter(new FileNameExtensionFilter("HTML files", //
				new String[] {
						"html", "htm"
				}));
		final int returnVal = chooser.showSaveDialog(null);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return;
		}
		final File file = chooser.getSelectedFile();
		final HTMLDocument doc = (HTMLDocument) editor.getDocument();
		final EditorKit kit = editor.getEditorKit();
		try (final FileWriter writer = new FileWriter(file)) {
			kit.write(writer, doc, 0, doc.getLength());
		} catch (BadLocationException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected static class InsertImageAction extends HTMLEditorKit.HTMLTextAction {
		private static final long serialVersionUID = 42L;

		public InsertImageAction() {
			super("insert-img");
		}

		private boolean isBlock(final Object a) {
			if (a instanceof HTML.Tag) {
				final HTML.Tag t = (HTML.Tag) a;
				return t.isBlock();
			}
			return false;
		}

		private HTML.Tag getBlockTag(final HTMLDocument doc, final int offset) {
			Element e = doc.getCharacterElement(offset);
			while (e != null && !isBlock(e.getAttributes().getAttribute(StyleConstants.NameAttribute))) {
				e = e.getParentElement();
			}
			if (e != null) {
				return (HTML.Tag) e.getAttributes().getAttribute(StyleConstants.NameAttribute);
			}
			return null;
		}

		private String getMimeOfImage(final File file) {
			String type = null;
			try {
				final URL u = file.toURI().toURL();
				final URLConnection uc = u.openConnection();
				type = uc.getContentType();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return type;
		}

		private String getImage(final File file) {
			final String mime = getMimeOfImage(file);
			if (mime == null) {
				return null;
			}
			try (final FileInputStream is = new FileInputStream(file)) {
				final Image img = Image.parse(mime, is);
				return "<img id=\"" + img.md5sum + "\" src=\"" + img.dataUri + "\">";
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		private void loadImage(final HTML.Tag blockTag, final ActionEvent ae) {
			final JFileChooser chooser = new JFileChooser();
			chooser.setRequestFocusEnabled(false);
			chooser.setCurrentDirectory(new File("."));
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileFilter(new FileNameExtensionFilter("Image files", //
					new String[] {
							"png", "gif", "jpeg", "jpg", "jpe", "jfif"
					}));
			final int returnVal = chooser.showOpenDialog(null);
			if (returnVal != JFileChooser.APPROVE_OPTION) {
				return;
			}
			final File file = chooser.getSelectedFile();
			final SwingWorker<Void, Void> worker = new SwingWorker<>() {
				private final AtomicReference<String> html = new AtomicReference<>();

				@Override
				public Void doInBackground() {
					html.set(getImage(file));
					return null;
				}

				@Override
				public void done() {
					new HTMLEditorKit.InsertHTMLTextAction("delegated", html.get(), blockTag, HTML.Tag.IMG)
							.actionPerformed(ae);
				}
			};
			worker.execute();
		}

		public void actionPerformed(final ActionEvent e) {
			final JTextPane editor = (JTextPane) getEditor(e);
			if (editor != null) {
				final HTMLDocument doc = getHTMLDocument(editor);
				final int offset = editor.getSelectionStart();
				final HTML.Tag blockTag = getBlockTag(doc, offset);
				if (blockTag != null) {
					loadImage(blockTag, e);
				}
			}
		}
	}

	protected static class StrikethroughAction extends StyledEditorKit.StyledTextAction {
		private static final long serialVersionUID = 42L;

		public StrikethroughAction() {
			super("font-strike");
		}

		public void actionPerformed(final ActionEvent e) {
			final JEditorPane editor = getEditor(e);
			if (editor != null) {
				final StyledEditorKit kit = getStyledEditorKit(editor);
				final MutableAttributeSet attr = kit.getInputAttributes();
				final boolean strike = (StyleConstants.isStrikeThrough(attr)) ? false : true;
				final SimpleAttributeSet sas = new SimpleAttributeSet();
				StyleConstants.setStrikeThrough(sas, strike);
				setCharacterAttributes(editor, sas, false);
			}
		}
	}

	protected static class LinkAction extends HTMLEditorKit.HTMLTextAction {
		private static final long serialVersionUID = 42L;

		public LinkAction() {
			super("link-broken");
		}

		private void insertLink(final JTextPane editor, final String href) {
			final SimpleAttributeSet sasTag = new SimpleAttributeSet();
			final SimpleAttributeSet sasAttr = new SimpleAttributeSet();
			sasAttr.addAttribute(HTML.Attribute.HREF, href);
			sasTag.addAttribute(HTML.Tag.A, sasAttr);
			final int pss = editor.getSelectionStart();
			final int pse = editor.getSelectionEnd();
			if (pss != pse) {
				final HTMLDocument doc = getHTMLDocument(editor);
				doc.setCharacterAttributes(pss, pse - pss, sasTag, false);
			}
		}

		private String findLink(final JTextPane editor) {
			final int pss = editor.getSelectionStart();
			final int pse = editor.getSelectionEnd();
			final HTMLDocument doc = getHTMLDocument(editor);
			for (int i = pss; i <= pse; i++) {
				final Element elem = doc.getCharacterElement(i);
				final AttributeSet elemAttr = elem.getAttributes();
				final AttributeSet tagAttr = (AttributeSet) elemAttr.getAttribute(HTML.Tag.A);
				if (tagAttr != null) {
					final String href = (String) tagAttr.getAttribute(HTML.Attribute.HREF);
					if ((href != null) && !href.isEmpty()) {
						return href;
					}
				}
				i = elem.getEndOffset() - 1; // fast-forward
			}
			return null;
		}

		public void actionPerformed(final ActionEvent e) {
			final JTextPane editor = (JTextPane) getEditor(e);
			if (editor != null) {
				final String href = findLink(editor);
				final LinkDialog d = new LinkDialog(null);
				if ((href != null) && !href.isEmpty()) {
					d.setLink(href);
				}
				d.init();
				if (!d.isAccepted()) { // Cancel
					return;
				}
				insertLink(editor, d.getLink());
			}
		}
	}

	protected static class UnlinkAction extends HTMLEditorKit.HTMLTextAction {
		private static final long serialVersionUID = 42L;

		public UnlinkAction() {
			super("unlink");
		}

		public void actionPerformed(final ActionEvent e) {
			final JTextPane editor = (JTextPane) getEditor(e);
			if (editor != null) {
				final int pss = editor.getSelectionStart();
				final int pse = editor.getSelectionEnd();
				final HTMLDocument doc = getHTMLDocument(editor);
				for (int i = pss; i <= pse; i++) {
					final Element elem = doc.getCharacterElement(i);
					final AttributeSet elemAttr = elem.getAttributes();
					final AttributeSet tagAttr = (AttributeSet) elemAttr.getAttribute(HTML.Tag.A);
					if (tagAttr != null) {
						final SimpleAttributeSet newAttr = new SimpleAttributeSet(elem.getAttributes());
						newAttr.removeAttribute(HTML.Tag.A);
						doc.setCharacterAttributes(elem.getStartOffset(),
								elem.getEndOffset() - elem.getStartOffset(), newAttr, true);
					}
					i = elem.getEndOffset() - 1; // fast-forward
				}
			}
		}
	}

	protected static class IndentAction extends StyledEditorKit.StyledTextAction {
		private static final long serialVersionUID = 42L;
		private final float step;

		public IndentAction(final String name, final float step) {
			super(name);
			this.step = step; // +20f
		}

		public void actionPerformed(final ActionEvent e) {
			final JTextPane editor = (JTextPane) getEditor(e);
			if (editor != null) {
				final AttributeSet attr = editor.getParagraphAttributes();
				float leftIndent = StyleConstants.getLeftIndent(attr);
				final SimpleAttributeSet sas = new SimpleAttributeSet();
				StyleConstants.setLeftIndent(sas, Math.max(leftIndent + step, 0f));
				setParagraphAttributes(editor, sas, false);
			}
		}
	}

	protected static class CustomHTMLWriter extends HTMLWriter {
		private boolean inBody = false;
		private boolean inParagraph = false;
		private int paragraphText = 0;

		public CustomHTMLWriter(final Writer w, final HTMLDocument doc, final int pos, final int len) {
			super(w, doc, pos, len);
		}

		@Override
		protected boolean synthesizedElement(Element elem) {
			if (matchNameAttribute(elem.getAttributes(), HTML.Tag.IMPLIED)) {
				return false;
			}
			return false;
		}

		@Override
		protected void startTag(final Element elem) throws IOException, BadLocationException {
			if (matchNameAttribute(elem.getAttributes(), HTML.Tag.P)) {
				inParagraph = true;
				paragraphText = 0;
			} else if (matchNameAttribute(elem.getAttributes(), HTML.Tag.DIV)) {
				return;
			} else if (matchNameAttribute(elem.getAttributes(), HTML.Tag.IMPLIED)) {
				if (!inBody) {
					return;
				}
				indent();
				write('<');
				write("div");
				writeAttributes(elem.getAttributes());
				write('>');
				writeLineSeparator();
				return;
			} else if (matchNameAttribute(elem.getAttributes(), HTML.Tag.BODY)) {
				inBody = true;
			}
			super.startTag(elem);
		}

		@Override
		protected void text(final Element elem) throws BadLocationException, IOException {
			if (inParagraph) {
				int start = Math.max(getStartOffset(), elem.getStartOffset());
				int end = Math.min(getEndOffset(), elem.getEndOffset());
				if (start < end) {
					final String text = getDocument().getText(start, end - start);
					final boolean isBlank = text.isBlank();
					if (!isBlank) {
						paragraphText++;
					}
				}
			}
			super.text(elem);
		}

		@Override
		protected void endTag(final Element elem) throws IOException {
			if (matchNameAttribute(elem.getAttributes(), HTML.Tag.P)) {
				inParagraph = false;
				if (paragraphText == 0) {
					indent();
					write("&nbsp;");
				}
			} else if (matchNameAttribute(elem.getAttributes(), HTML.Tag.DIV)) {
				return;
			} else if (matchNameAttribute(elem.getAttributes(), HTML.Tag.IMPLIED)) {
				if (!inBody) {
					return;
				}
				indent();
				write("</div>");
				writeLineSeparator();
				return;
			} else if (matchNameAttribute(elem.getAttributes(), HTML.Tag.BODY)) {
				inBody = false;
			}
			super.endTag(elem);
		}
	}

	/**
	 * Standard Web Colors:
	 * aqua, black, blue, fuchsia, gray, green, lime, maroon, navy, olive, purple,
	 * red, silver, teal, white, and yellow
	 */
	public static class WebColor extends Color {
		private static final long serialVersionUID = 42L;
		public static final WebColor W_WHITE = new WebColor(0xFF, 0xFF, 0xFF, "white");
		public static final WebColor W_SILVER = new WebColor(0xC0, 0xC0, 0xC0, "silver");
		public static final WebColor W_GRAY = new WebColor(0x80, 0x80, 0x80, "gray");
		public static final WebColor W_BLACK = new WebColor(0x00, 0x00, 0x00, "black");
		public static final WebColor W_RED = new WebColor(0xFF, 0x00, 0x00, "red");
		public static final WebColor W_MAROON = new WebColor(0x80, 0x00, 0x00, "maroon");
		public static final WebColor W_YELLOW = new WebColor(0xFF, 0xFF, 0x00, "yellow");
		public static final WebColor W_OLIVE = new WebColor(0x80, 0x80, 0x00, "olive");
		public static final WebColor W_LIME = new WebColor(0x00, 0xFF, 0x00, "lime");
		public static final WebColor W_GREEN = new WebColor(0x00, 0x80, 0x00, "green");
		public static final WebColor W_AQUA = new WebColor(0x00, 0xFF, 0xFF, "aqua");
		public static final WebColor W_TEAL = new WebColor(0x00, 0x80, 0x80, "teal");
		public static final WebColor W_BLUE = new WebColor(0x00, 0x00, 0xFF, "blue");
		public static final WebColor W_NAVY = new WebColor(0x00, 0x00, 0x80, "navy");
		public static final WebColor W_FUCHSIA = new WebColor(0xFF, 0x00, 0xFF, "fuchsia");
		public static final WebColor W_PURPLE = new WebColor(0x80, 0x00, 0x80, "purple");

		private final String name;

		public WebColor(final int r, final int g, final int b, final String name) {
			super(r, g, b);
			this.name = name;
		}

		public String getWebName() {
			return name;
		}
	}

	public static class ColorSelectorButton extends JButton {
		private static final long serialVersionUID = 42L;
		private final SetColorActionFactory colorFactory;
		private final JPopupMenu menuColor;

		public ColorSelectorButton() {
			this(new SetColorActionFactory());
		}

		public ColorSelectorButton(final SetColorActionFactory colorFactory) {
			this.colorFactory = colorFactory;
			this.menuColor = createPopup();
			setSelectedColor(WebColor.W_BLACK);
			//
			final Component comp = this;
			addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent ae) {
					menuColor.show(comp, 0, comp.getHeight());
				}
			});
		}

		protected final JPopupMenu createPopup() {
			JPopupMenu menuColor = new JPopupMenu();
			menuColor.setRequestFocusEnabled(false);
			menuColor.setLayout(new GridLayout(8, 2));
			final WebColor[] colors = {
					WebColor.W_WHITE, //
					WebColor.W_SILVER, //
					WebColor.W_GRAY, //
					WebColor.W_BLACK, //
					WebColor.W_RED, //
					WebColor.W_MAROON, //
					WebColor.W_YELLOW, //
					WebColor.W_OLIVE, //
					WebColor.W_LIME, //
					WebColor.W_GREEN, //
					WebColor.W_AQUA, //
					WebColor.W_TEAL, //
					WebColor.W_BLUE, //
					WebColor.W_NAVY, //
					WebColor.W_FUCHSIA, //
					WebColor.W_PURPLE //
			};
			for (final WebColor c : colors) {
				JMenuItem menuItem = new JMenuItem(c.getWebName());
				menuItem.addActionListener(e -> setSelectedColor(c, e));
				IconHelper.set(MaterialDesignW.WATER, menuItem, c);
				menuColor.add(menuItem);
			}
			return menuColor;
		}

		public void setSelectedColor(final WebColor newColor) {
			final Icon icon = getIcon();
			final String text = getText();
			setAction(colorFactory.create(newColor));
			setIcon(icon);
			setText(text);
		}

		public void setSelectedColor(final WebColor c, final ActionEvent e) {
			setSelectedColor(c);
			getAction().actionPerformed(e);
		}

		public static class SetColorActionFactory {
			public StyledEditorKit.StyledTextAction create(final WebColor newColor) {
				return new StyledEditorKit.ForegroundAction(newColor.getWebName(), newColor);
			}
		}
	}

	public static class Image {
		public final String md5sum;
		public final String type;
		public final String dataUri;

		public Image(final String md5sum, final String type, final String base64) {
			this.md5sum = md5sum;
			this.type = type;
			this.dataUri = base64;
		}

		public static Image parse(final String type, final InputStream is) {
			final StringWriter out = new StringWriter();
			out.append("data:").append(type).append(";base64,");
			try (final MD5OutputStream os = new MD5OutputStream(new Base64OutputStream(
					new WriterOutputStream(out, StandardCharsets.UTF_8), true, 0, null))) {
				IOUtils.copy(is, os);
				os.flush();
				os.close();
				final String md5sum = os.getHexHash();
				return new Image(md5sum, type, out.getBuffer().toString());
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				IOUtils.closeQuietly(is);
			}
		}

		public static class MD5OutputStream extends FilterOutputStream {
			private final MessageDigest md5 = DigestUtils.getMd5Digest();
			private byte[] hash = null;

			public MD5OutputStream(final OutputStream out) {
				super(out);
			}

			@Override
			public void write(final byte[] b) throws IOException {
				md5.update(b);
				super.write(b);
			}

			@Override
			public void write(final byte[] b, final int off, final int len) throws IOException {
				md5.update(b, off, len);
				super.write(b, off, len);
			}

			@Override
			public void write(final int b) throws IOException {
				md5.update((byte) b);
				super.write(b);
			}

			@Override
			public void close() throws IOException {
				if (hash == null) {
					hash = md5.digest();
				}
				super.close();
			}

			public byte[] getHash() {
				return hash;
			}

			public String getHexHash() {
				return Hex.encodeHexString(hash);
			}
		}
	}

	public static class CustomHTMLEditorKit extends HTMLEditorKit {
		private static final long serialVersionUID = 42L;
		private final AtomicBoolean init = new AtomicBoolean();

		// https://community.oracle.com/tech/developers/discussion/1363299/detect-when-image-finishes-loading-in-html-with-jeditorpane
		private final ViewFactory factory = new HTMLFactory() {
			public View create(final Element elem) {
				final View v = super.create(elem);
				if ((v != null) && (v instanceof ImageView)) {
					final String src = (String) elem.getAttributes().getAttribute(HTML.Attribute.SRC);
					if (src == null) {
						return null;
					}
					final boolean isDataURI = src.startsWith("data:");
					if (isDataURI) {
						return new ImageView(elem) {
							@Override
							public URL getImageURL() {
								try {
									return DataURLHandler.createURL(null, src);
								} catch (MalformedURLException e) {
									return null;
								}
							}
						};
					}
					// img.setLoadsSynchronously(true);
				}
				return v;
			}
		};

		@Override
		public ViewFactory getViewFactory() {
			return factory;
		}

		@Override
		public void read(final InputStream in, final Document doc, final int pos) //
				throws IOException, BadLocationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void write(final OutputStream out, final Document doc, final int pos, final int len) //
				throws IOException, BadLocationException {
			throw new UnsupportedOperationException();
		}

		protected String sanitizeHTML(final String input) {
			return HTML_POLICY_DEFINITION_BASIC.sanitize(input);
		}

		@Override
		public void read(final Reader in, final Document doc, final int pos) //
				throws IOException, BadLocationException {
			if (init.getAndSet(true)) {
				final String text = sanitizeHTML(IOUtils.toString(in));
				super.read(new StringReader(text), doc, pos);
			} else {
				super.read(in, doc, pos);
			}
		}

		@Override
		public void write(final Writer out, final Document doc, final int pos, final int len) //
				throws IOException, BadLocationException {
			final CustomHTMLWriter w = new CustomHTMLWriter(out, (HTMLDocument) doc, pos, len);
			w.write();
			out.flush();
		}
	}
}
