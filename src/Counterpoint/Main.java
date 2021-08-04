package Counterpoint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import static java.lang.System.out;

public class Main {
    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static String songFile;
    //0 2 4 5 6 7 9
    private static String observationFile = "src/MIDI files/firstSpecies/burg10.mid";
    private static final Random random = new Random();
    public static final int NOTE_ON = 0x90;
    private static String[] states = {"PU","m3","M3","P5","m6","M6","P8", "m10", "M10"};
    private static String[] states2 = {"PU", "m2u", "m2d", "M2u", "M2d", "m3u", "m3d", "M3u", "M3d", "P4u", "P4d", "P5u", "P5d", "m6u", "m6d", "M6u", "M6d", "m7u", "m7d", "M7u", "M7d", "P8u", "P8d"};
    static ArrayList<int[][]> songs = new ArrayList<>();
//    static ArrayList<int[][]> secondSpecies = new ArrayList<>();
//    static ArrayList<int[]> secondSpeciesHarmonic = new ArrayList<>();
//    static ArrayList<int[]> allowableIntervals = new ArrayList<>();
    static HashMap <Integer, Integer> hmHarmonic = new HashMap <Integer,Integer>();
    static HashMap <Integer, Integer> hmMelodic = new HashMap <Integer,Integer>();
    static HashMap<Integer, String> hmObservation = new HashMap<Integer, String>();
    static HashMap <String, Integer> hmNotes = new HashMap <String,Integer>();
    static double[][] transitionP = new double[hmNotes.size()][hmNotes.size()];


    public static void main(String[] args)  throws Exception{
        System.out.println("hello cindy");
        //Adds examples with counterpoint in top voice
        for(int i=0; i<10; i++){
            songFile = "src/MIDI files/firstSpecies/cp" +i+".mid";
            Sequence sequence = MidiSystem.getSequence(new File(songFile));
            songs.add(midiTo2DArray(sequence));
        }
        System.out.println("First Species Notes: ");
        printNote(songs);
        System.out.println("-----------");
        printNote(songs);

        //sets up hashmaps that are used to get indices for intervals
        setHarmonicHashMap();
        setNoteMap();
        setMelodicHashMap();
        setObservationHm();
        //buildAllowableIntervals();

        double [] startP = convertArrayProb(getStartFreqNew());

        int[][] transitionMatrix = getTransitionProbNew();


        //adds 2nd species probabilities to transition and emission matrices
        //secondSpeciesProb(transitionMatrix, emissionMatrix);
        transitionP = convertProbability(transitionMatrix, hmNotes.size(), hmNotes.size());


        //for notes as observed states
        int[][] emissionMatrix = getEmissionProb1New();
        System.out.println("\nEmission Probabilities: ");
        print2DArray(emissionMatrix);
        double [][] emissP = convertProbability(emissionMatrix,  hmNotes.size(),states.length);




        // for melodic interval as observed states
//        int[][] emissionMatrix = getEmissionProb();
//        System.out.println("\nEmission Probabilities: ");
//        print2DArray(emissionMatrix);
//        double [][] emissP = convertProbability(emissionMatrix, states.length, hmMelodic.size());





        // observation file
        Sequence observationSeq = MidiSystem.getSequence(new File(observationFile));

        System.out.println("------Forward Algorithm------");

        //comment one of these method call to calculate the forward probability
        double final_prob = getForwardProb_noteNew(observationSeq,startP, emissP);
        //double final_prob = getForwardProb_melodic(observationSeq,startP, emissP);
        System.out.println("The final probability for the given observation is  "+ final_prob);






    }
    //takes in a sequence for an example and extracts note values to 2D array
    private static int[][] midiTo2DArray(Sequence sequence){
        int numNotes = 0;
        int numVoices = 0;
        ArrayList<Integer> notesPerTrack = new ArrayList<>();
        //first loop to find number of notes in each voice
        for (Track track : sequence.getTracks()) {
            numNotes = 0;
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    if (sm.getCommand() == NOTE_ON) {
                        int key = sm.getData1();
                        int octave = (key / 12) - 1;
                        int note = key % 12;
                        String name = NOTE_NAMES[note];
                        numNotes++;
                    }
                }
            }
            notesPerTrack.add(numNotes);
            numVoices++;
        }

        //Finds largest number of notes in notesPerTrack
        int maxNotes= 0;
        int maxIndex = 0;
        for(int i=0; i<notesPerTrack.size(); i++){
            int curr = notesPerTrack.get(i);
            if(curr > maxNotes){
                maxNotes = curr;
                maxIndex = i;
            }
        }

        //second loop to add notes to 2D array
        int trackCount = 0;
        int[][] midiNotes = new int[numVoices-1][maxNotes];
        for (Track track : sequence.getTracks()) {
            int index = 0;
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    if (sm.getCommand() == NOTE_ON) {
                        int key = sm.getData1();
                        //int octave = (key / 12) - 1;
                        //int note = key % 12;
                        //String name = NOTE_NAMES[note];
                        if(trackCount > 0) {
                            if(maxIndex > 1){
                                if(trackCount == 1){
                                    midiNotes[1][index] = key;
                                    index++;
                                }else if(trackCount == 2){
                                    midiNotes[0][index] = key;
                                    index++;
                                }
                            }else{
                                midiNotes[trackCount - 1][index] = key;
                                index++;
                            }
                        }
                    }
                }
            }
            trackCount++;
        }
        return midiNotes;
    }

    //This method convert note in integer to string and print out the notes
    public static void printNote(ArrayList<int[][]> list){
        System.out.println("Our songs' notes in integer");
        for(int i = 0;i < list.size();i++){
            int numNotes = list.get(i)[0].length;
            for(int j = 0;j < list.get(i).length;j++){
                for(int k = 0; k < numNotes;k++){
                    int key = list.get(i)[j][k];
                    System.out.print(key+" ");
                }
                System.out.println();
            }
        }

        System.out.println("Our songs' notes in string");
        for(int i = 0;i < list.size();i++){
            for(int j = 0;j < list.get(i).length;j++){
                int numNotes = list.get(i)[0].length;
                for(int k = 0; k < numNotes;k++){
                    int key = list.get(i)[j][k];
                    int octave = (key / 12) - 1;
                    int note = key % 12;
                    if(octave != -1){
                        String name = NOTE_NAMES[note];
                        System.out.print(name+octave+" ");
                    } else{
                        System.out.print("0 ");
                    }

                }
                System.out.println();
            }
        }
    }

    public static void setHarmonicHashMap(){
        hmHarmonic.put(0,0);
        hmHarmonic.put(3,1);
        hmHarmonic.put(4,2);
        hmHarmonic.put(7,3);
        hmHarmonic.put(8,4);
        hmHarmonic.put(9,5);
        hmHarmonic.put(12,6);
        hmHarmonic.put(15,7);
        hmHarmonic.put(16,8);

    }

    /*HashMap for melodic intervals. Each key is number of half steps and
    each value is the corresponding index. Negative values represent melodic
    intervals up and positive are melodic intervals down.
     */
    public static void setMelodicHashMap(){
        hmMelodic.put(0,0);
        hmMelodic.put(-1,1);
        hmMelodic.put(1,2);
        hmMelodic.put(-2,3);
        hmMelodic.put(2,4);
        hmMelodic.put(-3,5);
        hmMelodic.put(3,6);
        hmMelodic.put(-4,7);
        hmMelodic.put(4,8);
        hmMelodic.put(-5,9);
        hmMelodic.put(5,10);
        hmMelodic.put(-7,11);
        hmMelodic.put(7,12);
        hmMelodic.put(-8,13);
        hmMelodic.put(8,14);
        hmMelodic.put(-9,15);
        hmMelodic.put(9,16);
        hmMelodic.put(-12,17);
        hmMelodic.put(12,18);
    }
    public static void setNoteMap(){
        hmNotes.put("C",0);
        hmNotes.put("D",1);
        hmNotes.put("E",2);
        hmNotes.put("F",3);
        hmNotes.put("G",4);
        hmNotes.put("A",5);
        hmNotes.put("B",6);
    }


    /* HashMap to format observation sequence for Viterbi algorithm.
    Each key is the number of half steps and direction for the corresponding
    interval and each value is the interval in String form.
     */
    public static void setObservationHm(){
        hmObservation.put(0,"PU");
        hmObservation.put(-1,"m2u");
        hmObservation.put(1,"m2d");
        hmObservation.put(-2,"M2u");
        hmObservation.put(2,"M2d");
        hmObservation.put(-3,"m3u");
        hmObservation.put(3,"m3d");
        hmObservation.put(-4,"M3u");
        hmObservation.put(4,"M3d");
        hmObservation.put(-5,"P4u");
        hmObservation.put(5,"P4d");
        hmObservation.put(-7,"P5u");
        hmObservation.put(7,"P5d");
        hmObservation.put(-8,"m6u");
        hmObservation.put(8,"m6d");
        hmObservation.put(-9,"M6u");
        hmObservation.put(9,"M6d");
        hmObservation.put(-12,"P8u");
        hmObservation.put(12,"P8d");
        hmObservation.put(23,"m3");
        hmObservation.put(24,"M3");
        hmObservation.put(27,"P5");
        hmObservation.put(28,"m6");
        hmObservation.put(29,"M6");
        hmObservation.put(32,"P8");
        hmObservation.put(35,"m10");
        hmObservation.put(36,"M10");
    }

    public static int [] getStartFreq(){
        /*Array to track number of times a harmonic interval follows
        the start harmonic interval. Array holds raw number data.
        Each index in the array represents a possible harmonic interval.
        */
        int[] freqArray = new int[states.length];
        for(int i = 0;i < songs.size();i++){
            int firstInterval = Math.abs((songs.get(i)[0][0])-(songs.get(i)[1][0]));
            freqArray[hmHarmonic.get(firstInterval)]++;
        }
        System.out.println("\nStart transition frequencies: ");
        for(int i = 0;i < freqArray.length;i++){
            System.out.print(freqArray[i]+" ");
        }
        System.out.println();
        return freqArray;
    }

    public static int [] getStartFreqNew(){
        /*Array to track number of times a harmonic interval follows
        the start harmonic interval. Array holds raw number data.
        Each index in the array represents a possible harmonic interval.
        */
        int[] freqArray = new int[hmNotes.size()];
        for(int i = 0;i < songs.size();i++){
            int firstNoteInt = songs.get(i)[1][0];
            int note = firstNoteInt % 12;
            String firstNote = NOTE_NAMES[note];
            freqArray[hmNotes.get(firstNote)]++;
        }
        System.out.println("\nStart transition frequencies: ");
        for(int i = 0;i < freqArray.length;i++){
            System.out.print(freqArray[i]+" ");
        }
        System.out.println();
        return freqArray;
    }

    //Converts an array of integers into an array of percentagres/frequwencies
    public static double[] convertArrayProb(int[] frequencies){
        double[] arrayProb = new double[frequencies.length];
        int total = 0;
        for(int i=0; i < frequencies.length; i++){
            total += frequencies[i];
        }
        if(total != 0){
            for(int j=0; j< frequencies.length; j++){
                arrayProb[j] = (frequencies[j]*1.0)/ total;
                System.out.print(arrayProb[j] + ", ");
            }
        }
        System.out.println();
        return arrayProb;
    }

    /* Returns a 2D array that counts the number of times a harmonic interval
    is followed by another harmonic interval.
     */
    private static int[][] getTransitionProb(){
        int[][] a = new int[states.length][states.length];
        for(int i=0; i< songs.size(); i++){
            int numNotes = songs.get(i)[0].length;
            for(int j=1; j<(numNotes-1); j++){
                int currInterval = Math.abs((songs.get(i)[0][j]) - (songs.get(i)[1][j]));
                int nextInterval = Math.abs((songs.get(i)[0][j+1]) - (songs.get(i)[1][j+1]));
                if(hmHarmonic.get(currInterval)!= null && hmHarmonic.get(nextInterval) != null){
                    a[hmHarmonic.get(currInterval)][hmHarmonic.get(nextInterval)]++;
                    if(nextInterval==0){
                        a[hmHarmonic.get(currInterval)][hmHarmonic.get(nextInterval)]=0;
                    }
                }else{
                    System.out.println("\nIllegal harmonic interval " +currInterval+", "+nextInterval+" in song " + i + " and note " + j);
                }
            }
        }
        System.out.println("\nHarmonic transition frequencies: ");
        for(int i=0; i<a.length; i++){
            for(int j=0; j<a[i].length; j++){
                System.out.print(a[i][j] + " ");
            }
            System.out.println("");
        }
        return a;
    }

    private static int[][] getTransitionProbNew(){
        int[][] a = new int[hmNotes.size()][hmNotes.size()];
        for(int i=0; i< songs.size(); i++){
            int numNotes = songs.get(i)[0].length;
            for(int j=0; j<(numNotes-1); j++){
                int currNoteInt = songs.get(i)[1][j];
                int nextNoteInt = songs.get(i)[1][j+1];


                int note = currNoteInt % 12;
                String currNote = NOTE_NAMES[note];

                int notetemp = nextNoteInt % 12;
                String nextNote = NOTE_NAMES[notetemp];


                if(hmNotes.get(currNote)!= null && hmNotes.get(nextNote) != null){
                    a[hmNotes.get(currNote)][hmNotes.get(nextNote)]++;
                    /*
                    if(nextNote==0){
                        a[hmNotes.get(currNote)][hmNotes.get(nextNote)]=0;
                    }

                     */
                }else{
                    //System.out.println("\nIllegal harmonic interval " +currInterval+", "+nextInterval+" in song " + i + " and note " + j);
                }
            }
        }
        System.out.println("\nHarmonic transition frequencies: ");
        for(int i=0; i<a.length; i++){
            for(int j=0; j<a[i].length; j++){
                System.out.print(a[i][j] + " ");
            }
            System.out.println("");
        }
        return a;
    }


    //This method creates the emission table for HMM1 (notes as observed state)
    private static int[][] getEmissionProb1(){
        int[][]b = new int[states.length][hmNotes.size()];
        for(int i=0; i< songs.size(); i++){
            for(int j=0; j<songs.get(i)[0].length; j++){
                int harmonic = Math.abs((songs.get(i)[0][j]) - (songs.get(i)[1][j]));
                int noteCF = (songs.get(i)[1][j])%12;
                String noteNameCF = NOTE_NAMES[noteCF];
                int noteCP = (songs.get(i)[0][j])%12;
                String noteNameCP = NOTE_NAMES[noteCP];
                if(noteNameCF.length() == 1 && noteNameCP.length() == 1){
                    if(hmHarmonic.get(harmonic)!=null && hmNotes.get(noteNameCF)!=null){
                        b[hmHarmonic.get(harmonic)][hmNotes.get(noteNameCF)]++;
                    }
                } else if(noteNameCF.length()!= 1){
                    System.out.println("Musica ficta note in CF: " + noteNameCF + " and interval: " + states[hmHarmonic.get(harmonic)]);
                } else if(noteNameCP.length()!=1){
                    System.out.println("Musica ficta note in CP: " + noteNameCP + " and interval: " + states[hmHarmonic.get(harmonic)]);
                }
            }
        }
        return b;
    }
    private static int[][] getEmissionProb1New(){
        int[][]b = new int[hmNotes.size()][states.length];
        for(int i=0; i< songs.size(); i++){
            for(int j=0; j<songs.get(i)[0].length; j++){
                int harmonic = Math.abs((songs.get(i)[0][j]) - (songs.get(i)[1][j]));
                int noteCF = (songs.get(i)[1][j])%12;
                String noteNameCF = NOTE_NAMES[noteCF];
                int noteCP = (songs.get(i)[0][j])%12;
                String noteNameCP = NOTE_NAMES[noteCP];
                if(noteNameCF.length() == 1 && noteNameCP.length() == 1){
                    if(hmHarmonic.get(harmonic)!=null && hmNotes.get(noteNameCF)!=null){
                        b[hmNotes.get(noteNameCF)][hmHarmonic.get(harmonic)]++;
                    }
                } else if(noteNameCF.length()!= 1){
                    //System.out.println("Musica ficta note in CF: " + noteNameCF + " and interval: " + states[hmHarmonic.get(harmonic)]);
                } else if(noteNameCP.length()!=1){
                    //System.out.println("Musica ficta note in CP: " + noteNameCP + " and interval: " + states[hmHarmonic.get(harmonic)]);
                }
            }
        }
        return b;
    }

    /*Returns a 2D array that counts the number of times a melodic interval
    occurs while in a harmonic state.
     */
    private static int[][] getEmissionProb(){
        int[][] b = new int[states.length][hmMelodic.size()];
        for(int i=0; i<songs.size(); i++){
            int numNotes = songs.get(i)[0].length;
            for(int j=1; j<numNotes; j++){
                int harmonicInterval = Math.abs((songs.get(i)[0][j]) - (songs.get(i)[1][j]));
                int melodicInterval = (songs.get(i)[1][j-1]) - (songs.get(i)[1][j]);
                if(hmHarmonic.get(harmonicInterval)!=null && hmMelodic.get(melodicInterval)!=null){
                    b[hmHarmonic.get(harmonicInterval)][hmMelodic.get(melodicInterval)]++;
                }
            }
        }
        return b;
    }

    //This method is used for convert count frequency to Probability
    public static double[][] convertProbability(int [][] countMatrix,int row, int col){
        double [][] probMatrix = new double[row][col];

        for(int i = 0; i < row; i ++) {
            int runningtotal = 0;
            //count the total frequency in a given row
            for(int j = 0; j < col; j ++) {
                runningtotal += countMatrix[i][j];
            }
            //update the probability of each index
            if(runningtotal!=0){
                for(int j = 0; j < col; j ++) {
                    probMatrix[i][j] = countMatrix[i][j]*1.0/runningtotal;
                }
            }

        }
        for(int i=0; i<probMatrix.length; i++){
            for(int j=0; j<probMatrix[i].length; j++){
                System.out.print(String.format("%.2f", probMatrix[i][j]));
                System.out.print(" ");
            }
            System.out.println("");
        }
        System.out.println();
        return probMatrix;
    }
    //Takes in a 2D array of integers and prints each value
    public static void print2DArray(int[][] array){
        for(int i=0; i<array.length; i++){
            for(int j=0; j<array[i].length; j++){
                System.out.print(array[i][j] + "," + " ");
            }
            System.out.println("");
        }
    }

    public static int[] getObservationsNew(int[][] observations2DArray){
        int[] observation = new int[observations2DArray[0].length];
        for(int i=0; i<observations2DArray[0].length; i++){
            int currInterval = Math.abs((observations2DArray[0][i]) - (observations2DArray[1][i]));
            out.println(currInterval);
            if(currInterval>16){
                currInterval-=12;
            }
            if(currInterval==10){
                currInterval-=1;
            }

            observation[i] = hmHarmonic.get(currInterval);
        }
        for (int i = 0; i < observation.length; i++) {
            out.print(observation[i] + " ");
        }

        return observation;
    }
    /*This method takes in the sequence for the example MIDI file and
    converts it into an array of Strings representing the sequence of
    melodic intervals for that example's Cantus Firmus to be used in the
    Viterbi algorithm. MODIFIED VERSION!!
     */
    public static int[] getObservations(int[][] observations2DArray){
        int numNotes = 0;
        for(int j=0; j< observations2DArray[0].length; j++){
            if(observations2DArray[1][j]!=0){
                numNotes++;
            }
        }
        int[] observation = new int[numNotes-1];
        for(int i=1; i<observations2DArray[0].length; i++){
            if(observations2DArray[1][i-1]!=0 && observations2DArray[1][i]!=0){
                int interval = (observations2DArray[1][i-1]) - (observations2DArray[1][i]);
                observation[i-1] = hmMelodic.get(interval);
            }
        }
        out.println("hello");
        for (int i = 0; i < observation.length; i++) {
            out.print(observation[i] + " ");
        }
        return observation;
    }

    public static String[] getObservations1(int[][] observation){
        String[] obs = new String[observation[0].length];
        for(int i=0; i<observation[0].length; i++){
            // !!! 记得改回来 cf:0 cp:1
            int note = (observation[0][i])%12;
            obs[i] = NOTE_NAMES[note];
        }
        return obs;
    }

    public static double getForwardProb_notes(Sequence observationSeq,double [] startP,double [][] emissP){
        // observation file
        String[] observations = getObservations1(midiTo2DArray(observationSeq));
        for(int i = 0;i<observations.length;i++){
            System.out.print(observations[i]+" ");
        }

        //convert observation in string to int
        int[] obsfake = new int[observations.length];
        for(int i = 0; i < observations.length;i++){
            obsfake[i] = hmNotes.get(observations[i]);
            System.out.print(obsfake[i]+" ");
        }

        Forward obj = new Forward();
        double final_prob = obj.compute(obsfake, states, startP, transitionP, emissP);
        return final_prob;

    }

    public static double getForwardProb_melodic(Sequence observationSeq,double [] startP,double [][] emissP){
        //convert observation in string to int
        int[] observations = getObservations(midiTo2DArray(observationSeq));

        Forward obj = new Forward();
        double final_prob = obj.compute(observations, states, startP, transitionP, emissP);
        return final_prob;

    }
    public static double getForwardProb_noteNew(Sequence observationSeq,double [] startP,double [][] emissP){
        //convert observation in string to int
        int[] observations = getObservationsNew(midiTo2DArray(observationSeq));

        Forward obj = new Forward();
        String [] temp = {"C","D","E","F","G","A","B"};
        double final_prob = obj.compute(observations, temp, startP, transitionP, emissP);
        return final_prob;

    }



}
