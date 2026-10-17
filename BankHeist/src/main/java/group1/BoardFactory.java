package group1;
/**
 * Factory class responsible for creating game boards.
 *
 * The BoardFactory generates board layouts depending on the level.
 * This separates the board creation logic from the main game.
 * 
 * @author Daniil, Brian
 * @version 1.0
 */
public class BoardFactory 
{
    /**
     * Creates and returns a game board for the specified game level.
     * 
     * The board layout is represented as an array of strings where different
     * characters represent different objects on the board.
     * 
     * ' ' = empty cell
     * X = wall
     * E = exit
     * T = thief
     * G = guard
     * M = money
     * S = glue trap
     * D = ink dart
     * 
     * @param level the level number (integer) used to determine which board layout to generate
     * @return the initialized board instance for the given level
     * @throws IllegalArgumentException if the level number is not recognized
     */
    public Board makeBoard(int level) 
    {
        String[] boardMap = switch(level){
            case 1-> new String[] {
                "XXXXXXXXXXXXXXXXXXXXXXXXX",
                "X X X X X X X X X X X X X",
                "X X X     X     D     M X",
                "X X   X X X X X X   X X X",
                "X     X X   X X X X X X X",
                "X           G     S     X",
                "X   X X X X X X X   X X X",
                "X X X X X   X X X   X X X",
                "X     M                 X",
                "X X X X   X X   X X X X X",
                "X X   X   X X X X X X X X",
                "X     M        T  D    EX",
                "X X X X X X X X X X X X X",
                "XXXXXXXXXXXXXXXXXXXXXXXXX"
            };
            case 2 -> new String[] {
                "XXXXXXXXXXXXXXXXXXXXXXXXX",
                "XT    X       X        MX",
                "X XXX X XXXXX X XXXXX X X",
                "X X S   X   X   X   S X X",
                "X X XXXXX X XXXXX XXXXX X",
                "X X       X       X     X",
                "X XXXXXXX XXXXXXX X XXXXX",
                "X X       G       X X   X",
                "X X XXXXXXXXXXXXXXX X X X",
                "X X M     S     M   X D X",
                "X XXXXXXX X XXXXXXXXX X X",
                "X M       X            EX",
                "X XXXXXXXXXXXXXXXXXXXXX X",
                "XXXXXXXXXXXXXXXXXXXXXXXXX"
            };
            case 3 -> new String[] {
                "XXXXXXXXXXXXXXXXXXXXXXXXX",
                "XT M   M  SXS  D   M  GSX",
                "X XXXDXXX XSX XXX XXX X X",
                "X M   X   M M   X   M   X",
                "XMXXXMXSXXXXXXX X XXX XXX",
                "X  T       D   SM       X",
                "XSXXXX XXX X XXX XXXXXXDX",
                "X     SM  S    M  D    SX",
                "X XXXX XXX X XXXDXXXXXS X",
                "XDXXX   XXDX XX   XXX XDX",
                "X M       M M       MD  X",
                "X XXX XXX XSX XXX XXXDXDX",
                "XS   M   MDXSM S M   M GE",
                "XXXXXXXXXXXXXXXXXXXXXXXXX"
            };
            case 4 -> new String[] {
                "XXXXXXXXXXXXXXXXXXXXXXXXX",
                "XT    X       X        MX",
                "X XXX X XMXXX X XXXXX X X",
                "X X S   X   X   X   S X X",
                "X X XXXXX X XX XX XXMXX X",
                "X M       X       X     X",
                "X XXMXXXX XXX XXX X XXXXX",
                "X X       G       X X   X",
                "X X XXXMXXXXXSXXXXX X X X",
                "X X M     S     M   S D X",
                "X XXXXXXX X XXXXSXXXX X X",
                "X M       X            EX",
                "X XXXMXXXXXXMXXXXXXXXXX X",
                "XXXXXXXXXXXXXXXXXXXXXXXXX"
            };
            case 5 -> new String[] {
                "XXXXXXXXXXXXXXXXXXXXXXXXX",
                "X  M S D E              X",
                "X  M S D                X",
                "X   T                   X",
                "X     XXXXX             X",
                "X     X   X             X",
                "X     X G X             X",
                "X     X   X             X",
                "X     XXXXX             X",
                "X                       X",
                "X                       X",
                "X                       X",
                "X                       X",
                "X                       X",
                "XXXXXXXXXXXXXXXXXXXXXXXXX"
            };
            default -> throw new IllegalArgumentException("Unknown level");
        };
        return Board.getInstance(boardMap);
    }
}
