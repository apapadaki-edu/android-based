package com.example.myapplication;

/**
 *
 * The user can input sequences of number for three function factorial, fibonacci and gcd
 *
 * The user can press a function button and give different inputs, what happens is:
 *
 * each sequence of numbers after the button is pressed are stored as a string in a list
 * a thread runs in the background and processes the sequences each at at time and when ready
 * the results are displayed on screed.
 *
 * the above process happens for all functions, the press of a button for one does not affect
 * the others.
 *
 * One can add different delays in thread result 'delivery' time, to see the threads work
 * in action. The delay can be modified in lines 90 to 99 for each loader separately.
 *
 */

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import android.content.Context;
import android.os.Bundle;;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Bundle>{

    // User input validation must be comma separated numbers (integers)
    private final String INPUT_VALIDATION_PATTERN = " *(?:(?:\\d+ *,)+ *)*(?:\\d+ *)";
    private LinearLayout parentResultsLinearLayout;
    private EditText parameters;
    LayoutInflater inflater;


    // Structures used caching user inputs (used as FIFO queues)
    LinkedList<String> inputsCashFibonacci = new LinkedList<String>();
    LinkedList<String> inputsCashGCD = new LinkedList<String>();
    LinkedList<String> inputsCashFactorial = new LinkedList<String>();


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

        factorialButton.setOnClickListener(view -> onFunctionChooseListener(inputsCashFactorial, 0));

        gcdButton.setOnClickListener(view -> onFunctionChooseListener(inputsCashGCD, 1));

        fibonacciButton.setOnClickListener(view -> onFunctionChooseListener(inputsCashFibonacci, 2));
    }



    // here new threads with the appropriate arguments are created be Loader Manager
    // inputs reference to cache, mode-function and delay value for the sleep function
    @Override
    public Loader<Bundle> onCreateLoader(int id, Bundle args) {
        switch (id)
        {
            case 0:
                return new CalculationLoader(this,args, inputsCashFactorial, "factorial", 1);
            case 1:
                return new CalculationLoader(this, args, inputsCashGCD, "gcd", 1);
            case 2:
                return new CalculationLoader(this, args, inputsCashFibonacci, "fibonacci",1);
            default:
                return null;
        }
    }

    // this function manages what happens after the threads have finished their job
    @Override
    public void onLoadFinished(Loader<Bundle> loader, Bundle data) {
        // in case of no data returned do nothing
        if (data == null)
        {
            return;
        }

        // create the view for displaying the results,
        int createdViewId = addFunctionResultsView();

        // take reference to that view
        TextView v = (TextView) findViewById(createdViewId);

        if (v != null)
        {
            // in case all went well attach the result in v
            v.setText(data.getString("result"));
        }


        // detect which loader sent the results (commenting added
        // for the 1st loader, the same applies to the others

        switch (loader.getId())
        {
            case 0:
                // remove last value from cache (user's first input)
                inputsCashFactorial.removeLast();

                // if the cache contains user input, make the thread
                // start loading again (the last value in cache)
                if(inputsCashFactorial.size() != 0)
                {
                    loader.startLoading();
                } else
                {
                    // otherwise kill the loader, its work is done
                    LoaderManager.getInstance(MainActivity.this).destroyLoader(0);
                }
                break;

            case 1:
                inputsCashGCD.removeLast();
                if(inputsCashGCD.size() != 0)
                {
                    loader.startLoading();
                } else
                {
                    LoaderManager.getInstance(MainActivity.this).destroyLoader(1);
                }
                break;

            case 2:
                inputsCashFibonacci.removeLast();
                if(inputsCashFibonacci.size() != 0)
                {
                    loader.startLoading();
                } else
                {
                    LoaderManager.getInstance(MainActivity.this).destroyLoader(2);
                }
                break;
            default:
                break;
        }
    }


    @Override
    public void onLoaderReset(Loader<Bundle> loader) { ;
        //TODO: default stub not implemented
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
    public void onFunctionChooseListener(LinkedList<String> cache, int loaderId){
        // user input
        String params = parameters.getText().toString().trim();

        // if there are not values given and there aren't comma separated numbers,
        // show toast and nothing else
        if (params.length() == 0 && !params.matches(INPUT_VALIDATION_PATTERN))
        {
            Toast.makeText(getApplicationContext(), getString(R.string.invalid_input_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        //  if the cache is empty or the same input is not given
        //  before the loader finishes its work
        // then add input parameters to cache
        if (cache.isEmpty() || !cache.getFirst().equals(params)){
            cache.addFirst(params);
        }

        // if the loader with loaderId already exists, replace previous callbacks
        // with new ones, otherwise create a loader with that id
        LoaderManager.getInstance(MainActivity.this)
                .initLoader(loaderId, null, MainActivity.this);

    }

}
