package com.example.myapplication;

/*

 * The user can input sequences of numbers for three functions factorial, fibonacci and gcd
 *
 * The user can press a function button and give different inputs. What happens is:
 *
 * After the button is pressed, each sequence of numbers are are fed to a thread 
 * that calculates the result in the background. Each time the result of a sequence is ready
 * the results are displayed on screen.
 *
 * The above process happens for all functions, the press of a button for one does not affect
 * the others.
 *
 * One can pass different delay numbers to threads, to see them work
 * in action. The delay can be modified in lines 77 to 81 for each function separately.
 *
 */

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{

    // prev regex" *(?:(?:\\d{1,3} *,)+ *)*(?:\\d{1,3} *)";
    private LinearLayout parentResultsLinearLayout;
    private EditText parameters;
    LayoutInflater inflater;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // reference to parent layout
        parentResultsLinearLayout = (LinearLayout) findViewById(R.id.display_results);
        parentResultsLinearLayout.removeAllViewsInLayout();

        // inflater for creating layout for results
        inflater = (LayoutInflater) getSystemService((Context.LAYOUT_INFLATER_SERVICE));

        // user input field
        parameters = (EditText)findViewById(R.id.parameters);

        // button references
        Button factorialButton = (Button) findViewById(R.id.button_factorial);
        Button gcdButton = (Button) findViewById(R.id.button_gcd);
        Button fibonacciButton = (Button) findViewById(R.id.button_fibonacci);
        Button clearButton = (Button) findViewById(R.id.button_clear);


        // clear button listener clears input field
        clearButton.setOnClickListener(view -> parameters.getText().clear());


        // custom listeners defined later on they take as inputs
        // a reference to the appropriate function's cache
        // and its loader's id (dedicated thread for that function)

        factorialButton.setOnClickListener(view -> onFunctionChooseListener("factorial","8"));

        gcdButton.setOnClickListener(view -> onFunctionChooseListener("gcd", "10"));

        fibonacciButton.setOnClickListener(view -> onFunctionChooseListener("fibonacci", "4"));
    }




    // inflater for creating the view containing a function results
    public int addFunctionResultsView()
    {
        final View factorialResultsView = inflater.inflate(R.layout.function_results_layout, null);

        int id = View.generateViewId();

        factorialResultsView.setId(id);
        parentResultsLinearLayout.addView(factorialResultsView,
                0);

        return id;
    }

    // listener template
    public void onFunctionChooseListener(String mode, String delay){
        // user input
        String params = parameters.getText().toString().trim();

        // if there are not values given and there aren't comma separated numbers,
        // show toast and nothing else
        // User input validation must be comma separated numbers (integers)
        String INPUT_VALIDATION_PATTERN = "^(?:[\\d]{1,4},)+[^,]{1,4}$";
        if (params.length() == 0 || !params.matches(INPUT_VALIDATION_PATTERN))
        {
            Log.i("In not matced", "Herre");
            Toast.makeText(getBaseContext(), getString(R.string.invalid_input_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }


        // if the loader with loaderId already exists, replace previous callbacks
        // with new ones, otherwise create a loader with that id
        new CalculateTask().execute(mode, params, delay);

    }

    // params String[] where 0th element is the function and 1st the number sequence to calculate
    // also there is a last parameter for thread testing, it delays the computation by some seconds
    // to see if the UI freezes
    @SuppressLint("StaticFieldLeak")
    private class CalculateTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String...params) {
            StringBuilder result = new StringBuilder();

            // redundant, just for testing
            try {
                TimeUnit.SECONDS.sleep(Integer.parseInt(params[2]));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (params[0].equals("gcd")) {
                return gcd(params[1]);
            }

            for (String num: params[1].split(",")) {
                if (params[0].equals("fibonacci")) {
                    result.append(fibonacci(num.trim())).
                            append(System.getProperty("line.separator"));
                    continue;
                }
                result.append(factorial(num.trim())).
                        append(System.getProperty("line.separator"));
            }

            return result.toString();

        }

        protected void onPostExecute(String result) {
            // create the view for displaying the results,
            int createdViewId = addFunctionResultsView();

            // take reference to that view
            TextView v = (TextView) findViewById(createdViewId);
            v.setText(result);
        }
    }

    /*
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
     */

    private String factorial(String number) {
        StringBuilder result = new StringBuilder();

        BigInteger factorial = new BigInteger(number);
        for (int i=2; i <= Long.parseLong(number); i++){
            try {
                factorial = factorial.multiply(BigInteger.valueOf(i));
            } catch (StackOverflowError | OutOfMemoryError | NumberFormatException e) {
                result.append(": Large Result Number: not enough memory!");
            }
        }

        result.append(number).append( " != ").append(factorial);

        return result.toString();
    }

    private String fibonacci(String number) {

        StringBuilder result = new StringBuilder();

        result.append("(F").append(number).append(") = {");

        BigInteger Fn_2 = BigInteger.valueOf(0);
        result.append(Fn_2).append(",");

        BigInteger Fn_1 = BigInteger.valueOf(1);
        result.append(Fn_1).append(",");

        String delimiter = "";
        for (int j = 2; j < Integer.parseInt(number.trim()); j++) {
            result.append(delimiter); //do not add delimiter as its added already
            delimiter = ","; // then set the delimiter to add it for the next sequence number
            try {
                BigInteger Fn = Fn_1.add(Fn_2);
                result.append(Fn);

                Fn_2 = Fn_1;
                Fn_1 = Fn;
            } catch (StackOverflowError | OutOfMemoryError | NumberFormatException e) {
                result.append(": Large Number: not enough memory!");
            }
        }
        result.append("}");

        return result.toString();
    }

    private String gcd(String numSequence) {
        String[] numbers = numSequence.split(",");
        StringBuilder result = new StringBuilder();
        result.append("GCD(").append(numSequence).append(") = ");

        Long gcd_all = Long.valueOf(numbers[0].trim());
        for(int i =1; i< numbers.length; i++){

            gcd_all = gcd(gcd_all, Long.valueOf(numbers[i].trim()));

            // used to break the loop sooner
            // (if a gcd of 1 is found before all numbers get examined)
            if (gcd_all == 1) {
                result.append(1);
            }
        }

        result.append(gcd_all);

        return result.toString();
    }

    // calculated based on the Euclidean Algorithm
    private static Long gcd(Long a, Long b){
        return a == b? a : gcd(b, Math.abs(a-b));
    }
}
