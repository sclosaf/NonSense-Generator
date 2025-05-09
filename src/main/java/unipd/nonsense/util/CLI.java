package unipd.nonsense.util;

import java.util.*;

public class CLI
{
    private enum Command
    {
        GENERATESENTENCE, ANALYZESENTENCE, GENERATEANDANALYZESENTENCE, HELP, QUIT
    }

    private static Map<String, Command> commands = new HashMap<>();
    private boolean running;

    public CLI()
    {
        running = true;

        commands.put("GenerateSentence", Command.GENERATESENTENCE);
        commands.put("AnalyzeSentence", Command.ANALYZESENTENCE);
        commands.put("GenerateAndAnalyzeSentence", Command.GENERATEANDANALYZESENTENCE);
        commands.put("Help", Command.HELP);
        commands.put("Quit", Command.QUIT);

        welcome();
        usage();

        while(running==true)
        {    
            inputCatcher();
        }
    }

    public void welcome()
    {
        System.out.println("Welcome to");
        System.out.println(" __  __                                                             ____                                          __                           ");
        System.out.println("/\\ \\/\\ \\                                                           /\\  _`\\                                       /\\ \\__                       ");
        System.out.println("\\ \\ `\\\\ \\    ___     ___     ____     __    ___     ____     __    \\ \\ \\L\\_\\     __    ___      __   _ __    __  \\ \\ ,_\\   ___   _ __          ");
        System.out.println(" \\ \\ , ` \\  / __`\\ /' _ `\\  /',__\\  /'__`\\/' _ `\\  /',__\\  /'__`\\   \\ \\ \\L_L   /'__`\\/' _ `\\  /'__`\\/\\`'__\\/'__`\\ \\ \\ \\/  / __`\\/\\`'__\\        ");
        System.out.println("  \\ \\ \\`\\ \\/\\ \\L\\ \\/\\ \\/\\ \\/\\__, `\\/\\  __//\\ \\/\\ \\/\\__, `\\/\\  __/    \\ \\ \\/, \\/\\  __//\\ \\/\\ \\/\\  __/\\ \\ \\//\\ \\L\\.\\_\\ \\ \\_/\\ \\L\\ \\ \\ \\/         ");
        System.out.println("   \\ \\_\\ \\_\\ \\____/\\ \\_\\ \\_\\/\\____/\\ \\____\\ \\_\\ \\_\\/\\____/\\ \\____\\    \\ \\____/\\ \\____\\ \\_\\ \\_\\ \\____\\\\ \\_\\\\ \\__/.\\_\\\\ \\__\\ \\____/\\ \\_\\         ");
        System.out.println("    \\/_/\\/_/\\/___/  \\/_/\\/_/\\/___/  \\/____/\\/_/\\/_/\\/___/  \\/____/     \\/___/  \\/____/\\/_/\\/_/\\/____/ \\/_/ \\/__/\\/_/ \\/__/\\/___/  \\/_/         ");
        
    }

    private void usage()
    {
        System.out.println("");
        System.out.println("This is the list of commands:");
        System.out.println("1) Generate Sentence");
        System.out.println("Generate a nonsense sentence based on the syntax of a given random sentence");
        System.out.println("2) Analyze Sentence");
        System.out.println("Validate the sentence structure and provide the syntactic tree");
        System.out.println("3) Generate And Analyze Sentence");
        System.out.println("Generate a nonsense sentence based on the syntax of a given random sentence,\nvalidate the sentence structure and provide the syntactic tree");
        System.out.println("4) Help");
        System.out.println("Give the list of the commands and the definition");
        System.out.println("5) Quit");
        System.out.println("Terminate the program");
    }

    public void inputCatcher()
    {
        System.out.println();
        Scanner sc = new Scanner(System.in);
        String cmd = sc.nextLine();
        commandExecuter(analyzeCommand(cmd));
        sc.close();
    }
    
    private String analyzeCommand(String cmd)
    {
        String trimmedCmd = cmd.trim();                                                       //eliminate the spaces before the sentence
        String[] ctrl = trimmedCmd.split("[^A-Za-z]");                              //divide the string and throw away everything but letters
        List<String> sentence = new ArrayList<>(Arrays.asList(ctrl));                         //create a list for the words from the string
        String command = "";                                                                  
        for (int i = 0; i<sentence.size(); i++)                                               //for loop to assemble the given command
        {
            if(sentence.get(i)=="") continue;
            String element = sentence.get(i);                                                 //save the element in the i position
            String firstLetter = element.substring(0, 1).toUpperCase();   //make the first letter of the element uppercase
            String otherLetters = element.substring(1);                            //take the other letters
            String newElement = firstLetter + otherLetters;                                   //combine the letters
            command += newElement;                                                            //combine the previous element with the new
        }
        return command;                                                                       //return the unified command
    }

    private void commandExecuter(String cmd)
    {
        if(commands.containsKey(cmd))
        {
            switch(commands.get(cmd))
            {
                case GENERATESENTENCE:
                    System.out.println("Sentence generated");
                    inputCatcher();
                break;

                case ANALYZESENTENCE:
                    System.out.println("Sentence analyzed");
                    inputCatcher();
                break;

                case GENERATEANDANALYZESENTENCE:
                    System.out.println("Sentence generated and analyzed");
                    inputCatcher();
                break;

                case HELP: 
                    usage();
                    inputCatcher();
                break;

                case QUIT:
                    System.out.println("See you soon!");
                    running = false;
                break;

                default:
                    throw new IllegalArgumentException("Please insert a valid command");
            }
        }

        else
        {
            System.out.println("Please, insert a valid command");
            inputCatcher();
        }
    }

    public boolean isRunning()
    {
        return running;
    }
}