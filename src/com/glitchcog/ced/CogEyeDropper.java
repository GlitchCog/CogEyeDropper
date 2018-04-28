package com.glitchcog.ced;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * Color eye dropper hexadecimal heads up display
 * 
 * @author Matt Yanos
 */
public class CogEyeDropper extends JFrame implements ActionListener, MouseMotionListener, MouseListener
{
    private static final long serialVersionUID = 1L;

    public static void main(String[] args)
    {
        new CogEyeDropper();
    }

    private KeyStroke escapeKeyStroke;
    private KeyStroke copyKeyStroke;
    private KeyStroke holdKeyStroke;
    private KeyStroke magnifyOnKeyStroke;
    private KeyStroke magnifyOffKeyStroke;
    private KeyStroke helpKeyStroke;

    private MagnifyingGlass magView;

    private final String helpMessage = "<HTML><TABLE CELLPADDING=5>" + 
                                           "<TR><TD>Ctrl+C:</TD><TD>Copy</TD></TR>" + 
                                           "<TR><TD>SpaceBar:</TD><TD>Pause on current color / Unpause</TD></TR>" + 
                                           "<TR><TD>M:</TD><TD>Toggle magnifying viewer</TD></TR>" + 
                                           "<TR><TD>F1:</TD><TD>Help</TD></TR>" + 
                                           "<TR><TD>Escape:</TD><TD>Quit</TD></TR>" + 
                                       "</TABLE></HTML>";

    public CogEyeDropper()
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setAlwaysOnTop(true);
        setUndecorated(true);
        setTitle("Cog Eye Dropper");

        addMouseMotionListener(this);
        addMouseListener(this);

        magView = new MagnifyingGlass(this);

        escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        copyKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK, false);
        holdKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false);
        magnifyOnKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, false);
        magnifyOffKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_M, 0, true);
        helpKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0, false);

        Action keyclickAction = new AbstractAction()
        {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e)
            {
                if (e.getActionCommand() == null)
                {
                    magView.setVisible(false);
                    displayHelp();
                }
                else if (e.getActionCommand().charAt(0) == KeyEvent.VK_ESCAPE)
                {
                    if (magView.isVisible())
                        magView.setVisible(false);
                    else
                        System.exit(0);
                }
                else if (e.getActionCommand().charAt(0) == 3)
                {
                    String s = (colorValue == null ? null : colorValue.getText());
                    if (s != null)
                        if (!s.equals(""))
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(s.trim()), null);
                }
                else if (e.getActionCommand().charAt(0) == KeyEvent.VK_SPACE)
                {
                    hold = !hold;
                    magView.setVisible(false);
                }
                else if (e.getActionCommand().charAt(0) == 109)
                    magView.setVisible(false);

            }
        };

        Action keyupAction = new AbstractAction()
        {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e)
            {
                if (!magView.isVisible())
                {
                    magView.locateWindow();
                    magView.setVisible(true);
                }
            }
        };

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "KEYCLICK");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(copyKeyStroke, "KEYCLICK");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(holdKeyStroke, "KEYCLICK");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(magnifyOnKeyStroke, "KEYUP");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(magnifyOffKeyStroke, "KEYCLICK");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(helpKeyStroke, "KEYCLICK");
        getRootPane().getActionMap().put("KEYCLICK", keyclickAction);
        getRootPane().getActionMap().put("KEYUP", keyupAction);

        init();

        setVisible(true);
    }

    private Timer clock;
    private Robot rob;
    private BufferedImage bi;
    private JPanel colorPanel;
    private BorderLabel colorValue;

    private int transX;
    private int transY;
    private boolean pointerOverWindow;

    private boolean dark;
    private boolean hold = false;
    private int holdCount = 0;

    public void init()
    {
        try
        {
            rob = new Robot();
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(this, "Error creating the robot.", "Crash", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        colorPanel = new JPanel();
        colorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        colorPanel.setLayout(new GridLayout(1, 1));
        colorValue = new BorderLabel(getHexValue(colorPanel.getBackground()));
        colorValue.setVerticalTextPosition(JLabel.CENTER);
        colorValue.setHorizontalTextPosition(JLabel.CENTER);
        colorValue.setOpaque(true);
        colorValue.setBackground(Color.WHITE);
        colorValue.setFont(new Font("DialogInput", Font.BOLD, 12));
        colorPanel.add(colorValue);
        add(colorPanel);
        pack();

        clock = new Timer(16, this);
        clock.start();
    }

    public void displayHelp()
    {
        JOptionPane.showMessageDialog(this, helpMessage, "Help", JOptionPane.PLAIN_MESSAGE);
    }

    public void setColor(Color c)
    {
        colorPanel.setBackground(c);
        colorValue.setText(getHexValue(c));

        dark = (c.getRed() + c.getGreen() + c.getBlue() < 382);
    }

    public String getHexValue(Color c)
    {
        String hex = Integer.toHexString(c.getRGB() & 0x00FFFFFF).toUpperCase();
        while (hex.length() < 6)
            hex = "0" + hex;
        return " " + hex + " ";
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        pointerOverWindow = true;
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        final int snapToDist = 8;
        int x = (int) MouseInfo.getPointerInfo().getLocation().getX() - transX;
        int y = (int) MouseInfo.getPointerInfo().getLocation().getY() - transY;
        Dimension res = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

        if (Math.abs(x) < snapToDist)
            x = 0;
        else if (Math.abs((x + getWidth()) - res.width) < snapToDist)
            x = res.width - getWidth();
        if (Math.abs(y) < snapToDist)
            y = 0;
        else if (Math.abs((y + getHeight()) - res.height) < snapToDist)
            y = res.height - getHeight();

        setLocation(x, y);
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        pointerOverWindow = true;
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        pointerOverWindow = false;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        magView.setVisible(false);
        if (e.getButton() == MouseEvent.BUTTON1)
        {
            transX = e.getX();
            transY = e.getY();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (hold)
        {
            holdCount++;
            if (holdCount > 25)
            {
                dark = !dark;
                holdCount = 0;
                repaint();
            }
        }
        else if (!pointerOverWindow)
        {
            Point p = MouseInfo.getPointerInfo().getLocation();
            bi = rob.createScreenCapture(new Rectangle((int) p.getX(), (int) p.getY(), 1, 1));
            Color c = new Color(bi.getRGB(0, 0));
            if (!colorPanel.getBackground().equals(c))
                setColor(c);
        }
    }

    private class BorderLabel extends JLabel
    {
        private static final long serialVersionUID = 1L;

        private int borderSize;

        public BorderLabel(String text)
        {
            this(text, 2);
        }

        public BorderLabel(String text, int borderSize)
        {
            super(text);
            this.borderSize = borderSize;
        }

        @Override
        public Dimension getPreferredSize()
        {
            FontMetrics metrics = this.getFontMetrics(getFont());
            return new Dimension(metrics.stringWidth(getText()) + borderSize * 2 + getText().length(), metrics.getHeight());
        }

        @Override
        public void paintComponent(Graphics g)
        {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            char[] letters = getText().toCharArray();

            FontMetrics metrics = this.getFontMetrics(getFont());

            int h = metrics.getAscent();
            int x = borderSize;
            for (int i = 0; i < letters.length; i++)
            {
                char ch = letters[i];

                g.setColor((dark ? Color.BLACK : Color.WHITE));
                for (int j = 0; j < borderSize; j++)
                {
                    for (int k = 0; k < borderSize; k++)
                    {
                        g.drawString("" + letters[i], x - j, h - k);
                        g.drawString("" + letters[i], x, h - k);
                        g.drawString("" + letters[i], x + j, h - k);

                        g.drawString("" + letters[i], x - j, h);
                        g.drawString("" + letters[i], x, h);
                        g.drawString("" + letters[i], x + j, h);

                        g.drawString("" + letters[i], x - j, h + k);
                        g.drawString("" + letters[i], x, h + k);
                        g.drawString("" + letters[i], x + j, h + k);
                    }
                }

                g.setColor((dark ? Color.WHITE : Color.BLACK));
                g.drawString("" + letters[i], x, h);

                x += metrics.charWidth(ch) + 1;
            }

        }
    }

    private class MagnifyingGlass extends Window
    {
        private static final long serialVersionUID = 1L;

        BufferedImage shot;

        static final int shotSize = 32;

        static final int windowSize = 256;

        MagnifyingGlass(Frame owner)
        {
            super(owner);
            setUndecorated(true);
            setSize(windowSize, windowSize);
        }

        public void locateWindow()
        {
            Point pointerLoc = MouseInfo.getPointerInfo().getLocation();
            Point shotLoc = new Point((int) pointerLoc.getX() - shotSize / 2, (int) pointerLoc.getY() - shotSize / 2);
            shot = rob.createScreenCapture(new Rectangle((int) shotLoc.getX(), (int) shotLoc.getY(), shotSize, shotSize));
            pointerLoc.setLocation(pointerLoc.getX() - getWidth() / 2, pointerLoc.getY() - getHeight() / 2);
            setLocation(pointerLoc);
        }

        public void paint(Graphics g)
        {
            g.drawImage(shot, 0, 0, getWidth(), getHeight(), 0, 0, shotSize, shotSize, null);
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    }

}