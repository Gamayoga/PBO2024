import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import javax.swing.*;
import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;


public class MinesweeperGame {
    private static class GridTile extends JButton {
        private int r;
        private int c;

        public int getR() {
            return r;
        }
        public void setR(int r) {
            this.r = r;
        }
        public int getC() {
            return c;
        }
        public void setC(int c) {
            this.c = c;
        }
        public GridTile(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }
    enum Level {
        EASY(8, 8, 10, 70, 30),
        NORMAL(12, 16, 40, 50, 25),
        HARD(16, 30, 99, 40, 20);

        final int numRows;
        final int numCols;
        final int mineCount;
        final int tileSize;
        final int fontSize;

        Level(int numRows, int numCols, int mineCount, int tileSize, int fontSize) {
            this.numRows = numRows;
            this.numCols = numCols;
            this.mineCount = mineCount;
            this.tileSize = tileSize;
            this.fontSize = fontSize;
        }
    }

    int tileSize = 70;
    Level currentLevel = Level.EASY;

    int boardWidth = currentLevel.numCols * tileSize;
    int boardHeight = currentLevel.numRows * tileSize;

    JFrame frame = new JFrame("Minesweeper");
    JLabel textLabel = new JLabel();
    JPanel textPanel = new JPanel();
    JPanel boardPanel = new JPanel();
    JButton resetButton = new JButton("RESTART");

    GridTile[][] board;
    ArrayList<GridTile> mineList;
    Random random = new Random();

    int tilesClicked = 0;
    boolean gameOver = false;

    int gameTime = 0;
    Timer timer;

    int lastGameTime = 0;

    MinesweeperGame() {
        frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        textLabel.setFont(new Font("Comic Sans MS", Font.BOLD, 30));
        textLabel.setForeground(Color.GREEN);
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        textLabel.setText("Mines: " + currentLevel.mineCount + "     (Let's play)");
        textLabel.setOpaque(true);
        textLabel.setBackground(Color.BLACK);


        textPanel.setLayout(new BorderLayout());
        textPanel.add(textLabel);
        frame.add(textPanel, BorderLayout.NORTH);

        initializeBoard(currentLevel);

        timer = new Timer(1000, e -> {
            gameTime++;
            updateTimerLabel();
        });

        resetButton.addActionListener(e -> showLevelDialog());
        resetButton.addActionListener(e -> restart(currentLevel));
        resetButton.setFont(new Font("Comic Sans MS", Font.ITALIC, 15));
        resetButton.setBackground(Color.GREEN); // Line 44
        resetButton.setForeground(Color.BLACK); // Line 45

        textPanel.add(resetButton, BorderLayout.EAST);
        frame.add(boardPanel, BorderLayout.CENTER);

        frame.setVisible(true);
        showLevelDialog();
        setMines();
    }

    void initializeBoard(Level level) {
        board = new GridTile[level.numRows][level.numCols];
        boardPanel.setLayout(new GridLayout(level.numRows, level.numCols));

        tileSize = level.tileSize;

        for (int r = 0; r < level.numRows; r++) {
            for (int c = 0; c < level.numCols; c++) {
                GridTile tile = new GridTile(r, c);
                board[r][c] = tile;
                tile.setFocusable(false);
                tile.setMargin(new Insets(0, 0, 0, 0));

                tile.setFont(new Font("Arial Unicode MS", Font.PLAIN, level.fontSize));
                tile.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (gameOver) {
                            return;
                        }
                        GridTile tile = (GridTile) e.getSource();

                        if (e.getButton() == MouseEvent.BUTTON1) {
                            if (Objects.equals(tile.getText(), "")) {
                                if (mineList.contains(tile)) {
                                    revealMines();
                                } else {
                                    checkMine(tile.getR(), tile.getC());
                                }
                            }
                        } else if (e.getButton() == MouseEvent.BUTTON3) {
                            if (Objects.equals(tile.getText(), "") && tile.isEnabled()) {
                                tile.setText("🚩");
                            } else if (Objects.equals(tile.getText(), "🚩")) {
                                tile.setText("?");
                            } else if (Objects.equals(tile.getText(), "?")) {
                                tile.setText("");
                            }
                        }
                    }
                });
                boardPanel.add(tile);
            }
        }
    }

    void setMines() {
        mineList = new ArrayList<>();
        int mineLeft = currentLevel.mineCount;

        while (mineLeft > 0) {
            int r = random.nextInt(currentLevel.numRows);
            int c = random.nextInt(currentLevel.numCols);

            GridTile tile = board[r][c];
            if (!mineList.contains(tile)) {
                mineList.add(tile);
                mineLeft -= 1;
            }
        }
    }

    void showLevelDialog() {
        String[] levels = {"EASY", "NORMAL", "HARD"};
        String selectedLevel = (String) JOptionPane.showInputDialog(
                frame,
                "level selection :",
                "LEVEL",
                JOptionPane.QUESTION_MESSAGE,
                null,
                levels,
                levels[0]);

        if (selectedLevel != null) {
            Level lvl = switch (selectedLevel) {
                case "NORMAL" -> Level.NORMAL;
                case "HARD" -> Level.HARD;
                default -> Level.EASY;
            };

            restart(lvl);
        }
    }

    void revealMines() {
        timer.stop();

        if (!gameOver) {
            lastGameTime = gameTime;
        }

        for (GridTile tile : mineList) {
            tile.setText("💣");
        }

        try {
            File file = new File("./audio/explosion.wav");
            AudioInputStream stream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            clip.start();

            // sleep to allow enough time for the clip to play
            Thread.sleep(500);

            stream.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        gameOver = true;
        textLabel.setText("Game Over!   Last Time: " + lastGameTime + "s");
    }

    void checkMine(int r, int c) {
        updateTimerLabel();

        if (!timer.isRunning()) {
            timer.start();
        }

        if (r < 0 || r >= currentLevel.numRows || c < 0 || c >= currentLevel.numCols) {
            return;
        }

        GridTile tile = board[r][c];
        if (!tile.isEnabled()) {
            return;
        }
        tile.setEnabled(false);
        tilesClicked += 1;

        int minesFound = 0;

        //top 3
        minesFound += countMine(r - 1, c - 1); //top left
        minesFound += countMine(r - 1, c);     //top
        minesFound += countMine(r - 1, c + 1); //top right

        //left and right
        minesFound += countMine(r, c - 1); //left
        minesFound += countMine(r, c + 1); //right

        //bottom 3
        minesFound += countMine(r + 1, c - 1); //bottom left
        minesFound += countMine(r + 1, c);     //bottom
        minesFound += countMine(r + 1, c + 1); //bottom right

        if (minesFound > 0) {
            tile.setText(Integer.toString(minesFound));
        } else {
            tile.setText("");

            //top 3
            checkMine(r - 1, c - 1); //top left
            checkMine(r - 1, c);     //top
            checkMine(r - 1, c + 1); //top right

            //left and right
            checkMine(r, c - 1); //left
            checkMine(r, c + 1); //right

            //bottom 3
            checkMine(r + 1, c - 1); //bottom left
            checkMine(r + 1, c);     //bottom
            checkMine(r + 1, c + 1); //bottom right
        }

        if (tilesClicked == currentLevel.numRows * currentLevel.numCols - mineList.size()) {
            timer.stop();
            lastGameTime = gameTime;
            gameOver = true;
            textLabel.setText("Mines Cleared!   Last Time: " + lastGameTime + "s");
        }
    }

    int countMine(int r, int c) {
        if (r < 0 || r >= currentLevel.numRows || c < 0 || c >= currentLevel.numCols) {
            return 0;
        }
        if (mineList.contains(board[r][c])) {
            return 1;
        }
        return 0;
    }

    void updateTimerLabel() {
        textLabel.setText("Mines: " + currentLevel.mineCount + "   Time: " + gameTime + "s");
    }

    void restart(Level level) {
        currentLevel = level;
        boardWidth = currentLevel.numCols * currentLevel.tileSize;
        boardHeight = currentLevel.numRows * currentLevel.tileSize;

        frame.setSize(boardWidth, boardHeight);

        tileSize = currentLevel.tileSize;

        tilesClicked = 0;
        gameOver = false;
        gameTime = 0;
        timer.stop();

        boardPanel.removeAll();

        textLabel.setText("Mines: " + currentLevel.mineCount + "     (Let's play)");

        initializeBoard(currentLevel);
        setMines();

        frame.add(boardPanel, BorderLayout.CENTER);

        frame.revalidate();
    }
}