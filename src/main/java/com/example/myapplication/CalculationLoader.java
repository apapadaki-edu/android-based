package com.example.myapplication;


/**
 * Class that extent the AsyncTaskLoader Class, makes thread management easier
 *
 * There is no need for the user to do much.
 *
 * Even though configurations change the jobs executing on the background does
 * not get interrupted.
 *
 */


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.loader.content.AsyncTaskLoader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class CalculationLoader extends AsyncTaskLoader<Bundle> {

    // list of string (each string is a comma separated
    // sequence of numbers - user's input)
    private LinkedList<String> parameters;

    // string that decides which function is called
    private String mode;

    // sleep time for threading demonstration purposes
    private int delay;

    public CalculationLoader (Context context, Bundle args, LinkedList<String> parameters, String mode, int delay) {
        super(context);
        this.parameters = parameters;
        this.mode = mode;
        this.delay = delay;
    }

    @Override
    public Bundle loadInBackground() {

        String[] params = parameters.getLast().split(",");

        Bundle result = new Bundle();

        // based on mode's value the results of the appropriate function are returned
        switch (mode){
            case "factorial":
                result.putString("result", factorial(params));
                break;
            case "fibonacci":
                result.putString("result", fibonacciSequence(params));
                break;
            case "gcd":
                result.putString("result", "gcd(" + Arrays.toString(params) +
                        ") = " + gcd(params));
        }

        try {
            TimeUnit.SECONDS.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    protected void onStartLoading(){
        //in case something happens deliver whatever results you've cached
        if(parameters.getLast().equals("start"))
        {
            //go in onFinished
           deliverResult(new Bundle());
        }
        else
        {
            // go to doInBackground
            forceLoad();
        }
    }


    /**
     * Functions for calculation of Factorial, Fibonacci and greater common denominator
     *
     * Some notes:
     * For the concatenation of the results, there was the use of a String Builder
     * it is faster than string concatenation and more memory efficient (Mutable data type)
     *
     * Also the results of factorial and fibonacci functions are created as BigIntegers,
     * since up to 20! or 50! even longs aren't enough.
     *
     * !!!Caution!!!
     * The user shouldn't overdo it, otherwise the are stackoverflow crashes
     * and as a result so does the app
     *
     * */

    public String factorial(String[] params){
        StringBuilder result = new StringBuilder();
        Log.i("in Factorial loader", Arrays.toString(params));
        for (String p: params)
        {
            BigInteger factorial = BigInteger.valueOf(Integer.parseInt(p.trim()));
            result.append(p.trim()).append("! = ");
            for(int i=2; i <= Integer.parseInt(p.trim()); i++){
                factorial = factorial.multiply(BigInteger.valueOf(i));
            }
            result.append(factorial.toString()).
                    append(System.getProperty("line.separator"));
        }
        return result.toString().trim();
    }

    // calculated based on the Euclidean Algorithm
    public static int gcd(int a, int b){
        return a == b? a : gcd(b, Math.abs(a-b));
    }

    // takes advantage of the following property gcd(a,b,c) = gcd(a, gcd(b,c))
    public static int gcd(String[] numbers) {
        Log.i("GCD loader", Arrays.toString(numbers));

        int gcd_all = Integer.parseInt(numbers[0]);
        for (String i : numbers)
        {
            gcd_all = gcd(gcd_all, Integer.parseInt(i.trim()));

            // used to break the loop sooner
            // (if a gcd of 1 is found before all numbers get examined)
            if (gcd_all == 1)
            {
                return 1;
            }
        }
        return gcd_all;
    }

    public static String fibonacciSequence(String[] params){
        Log.i("Fibonacci loader", Arrays.toString(params));

        StringBuilder result = new StringBuilder();
        for(String param : params) {
            result.append("(F").append(param).append(") = {");

            BigInteger Fn_2 = BigInteger.valueOf(0);
            result.append(Fn_2).append(",");

            BigInteger Fn_1 = BigInteger.valueOf(1);
            result.append(Fn_1).append(",");

            String delimiter = "";
            for (int j = 2; j < Integer.parseInt(param); j++)
            {
                result.append(delimiter);
                delimiter = ",";

                BigInteger Fn = Fn_1.add(Fn_2);
                result.append(Fn);

                Fn_2 = Fn_1;
                Fn_1 = Fn;
            }

            result.append("}").append(System.getProperty("line.separator"));
        }

        return result.toString();
    }
}

