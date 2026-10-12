package code;

public class CaveExplorer extends GenericSearchProblem {


    public static String solve(String initString, String strategy) {

        return "";

    }
    public static void main(String[] args) {
        String input = "3,4;0,1;100,2;0,3,0,0;4,12,0,0;0,7,23,8;3,2;1,0;";
        String[] sections = input.split(";");
        for(int i=0;i<sections.length;i++){
            System.out.print(i + ": ");
            System.out.println(sections[i]);
        }
        String[] dimensions = sections[0].split(",");

        int height = Integer.parseInt(dimensions[0]);
        int width = Integer.parseInt(dimensions[1]);

        System.out.println("Height: " + height);
        System.out.println("Width: " + width);

        int [][] grid = new int[height][width];

        for(int row = 0; row < height ; row++){
            String[] cells = sections[row+3].split(",");

            for(int column = 0; column < width ; column ++){
                grid[row][column] = Integer.parseInt(cells[column]);
            }
        }
        System.out.println("Grid:");

        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                System.out.print(grid[row][column] + " ");
            }

            System.out.println();
        }
    }

}