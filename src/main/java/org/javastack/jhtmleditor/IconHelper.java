package org.javastack.jhtmleditor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.beans.Beans;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.UIManager;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.swing.FontIcon;

public class IconHelper {
	public static final int ICON_SIZE = 24; // FIXME: Hardcoded value

	public static Color getColor(final String id, final JComponent comp) {
		if (Beans.isDesignTime()) {
			return comp.getForeground();
		}
		return UIManager.getColor(id);
	}

	public static ImageIcon createRainbowHelperIcon(final int width, final int height) {
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		final int y = ICON_SIZE - 4;
		int n = 0;
		for (final Color c : new Color[] {
				Color.RED, //
				Color.YELLOW, //
				Color.GREEN, //
				Color.CYAN, //
				Color.BLUE, //
				Color.MAGENTA //
		}) {
			g.setColor(c);
			g.fillRect(n * 4, y, 4, 4);
			n++;
		}
		image.flush();
		g.dispose();
		return new ImageIcon(image);
	}

	public static ImageIcon createColorSelectorIcon(final Ikon ikonBase, //
			final Color colorBase, final Color colorHelper) {
		final ImageIcon iconBase = FontIcon.of(ikonBase, ICON_SIZE, colorBase).toImageIcon();
		if (colorHelper != null) {
			final ImageIcon iconHelper = FontIcon.of(MaterialDesignC.COLOR_HELPER, ICON_SIZE, colorBase)
					.toImageIcon();
			return mergeIcons(iconBase, iconHelper);
		} else {
			final ImageIcon iconRainbow = createRainbowHelperIcon(ICON_SIZE, ICON_SIZE);
			return mergeIcons(iconBase, iconRainbow);
		}
	}

	public static ImageIcon mergeIcons(final ImageIcon... icons) {
		int width = 0, height = 0;
		for (final ImageIcon ico : icons) {
			width = Math.max(width, ico.getIconWidth());
			height = Math.max(height, ico.getIconHeight());
		}
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		// UIManager.getIcon("Table.descendingSortIcon");
		final Graphics2D g = image.createGraphics();
		for (final ImageIcon ico : icons) {
			g.drawImage(ico.getImage(), 0, 0, null);
		}
		image.flush();
		g.dispose();
		return new ImageIcon(image);
	}

	public static void setColorHelper(final Ikon icon, final AbstractButton comp) {
		if (!Beans.isDesignTime()) {
			comp.setText(null);
		}
		final ImageIcon iconEnabled = createColorSelectorIcon(icon, //
				getColor("Button.foreground", comp), getColor("Button.foreground", comp));
		final ImageIcon iconRollover = createColorSelectorIcon(icon, //
				getColor("Button.foreground", comp), null);
		final ImageIcon iconDisabled = createColorSelectorIcon(icon, //
				getColor("Button.disabledText", comp), getColor("Button.foreground", comp));
		comp.setIcon(iconEnabled);
		comp.setRolloverIcon(iconRollover);
		comp.setDisabledIcon(iconDisabled);
	}

	public static void set(final Ikon icon, final AbstractButton comp) {
		if (!Beans.isDesignTime()) {
			comp.setText(null);
		}
		final FontIcon iconEnabled = FontIcon.of(icon, ICON_SIZE, //
				getColor("Button.foreground", comp));
		final FontIcon iconDisabled = FontIcon.of(icon, ICON_SIZE, //
				getColor("Button.disabledText", comp));
		comp.setIcon(iconEnabled);
		comp.setDisabledIcon(iconDisabled);
	}

	public static void set(final Ikon icon, final AbstractButton comp, final Color color) {
		final FontIcon iconEnabled = FontIcon.of(icon, ICON_SIZE, color);
		comp.setIcon(iconEnabled);
	}
}
