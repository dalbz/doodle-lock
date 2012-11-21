package edu.osu.cse.doodleLock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import android.gesture.Gesture;
import android.gesture.GestureStroke;
import android.util.Log;

/**
 * Representation of a "doodle" - a gesture used for authentication
 * 
 * @author David
 *
 */
public class Doodle {

	/**
	 * Contains numerical representation of training gestures for doodle
	 */
	ArrayList<double[]> numericalRep = new ArrayList<double[]>();

	/**
	 * Constructs a new doodle from a list of gestures
	 * 
	 * @param gestureList Training gestures for doodle
	 */
	public Doodle(ArrayList<Gesture> gestureList){
		
		// iterate over the provided gestuers and convert them to an array of doubles
		for(Gesture gesture : gestureList){
    		
    		double[] gestureValues = new double[72];
    		
    		ArrayList<GestureStroke> strokes = gesture.getStrokes();
    		
    		for(int i = 0; i < 12; i++){
    			
    			if(i < strokes.size()){
    				
    				GestureStroke currentStroke = strokes.get(i);
    				// 0 - Stroke Length
    				gestureValues[6*i + 0] = currentStroke.length; 
    				// 1 - Stroke start point
    				gestureValues[6*i + 1] = currentStroke.points[0];
    				// 2 - Stroke end point
    				gestureValues[6*i + 2] = currentStroke.points[currentStroke.points.length - 1];
    				// 3 - Stroke width
    				gestureValues[6*i + 3] = currentStroke.boundingBox.width();
    				// 4 - Stroke height
    				gestureValues[6*i + 4] = currentStroke.boundingBox.height();
    				
    				// Disclaimer: The code below is really bad practice and should not reflect on 
    				// 		my abilities as a programmer. - David
    				long duration = 0;
    				
    				try {
						Field f = currentStroke.getClass().getDeclaredField("timestamps");
						f.setAccessible(true);
						
						long[] timestamps = (long[]) f.get(currentStroke);
						duration = timestamps[timestamps.length - 1] - timestamps[0];
					} catch (Exception e) {
						// That's right, I am catching  any and all exceptions. I'm like a honey badger. 
						// What are we gonna do about these exceptions? Nothing.
						Log.e("ERROR", "Reflection didn't work");
					}
    				
    				// 5 - Stroke Duration (time)
    				gestureValues[6*i + 5] = duration;
    			
    			}
    			else {
    				for(int j = 0; j < 6; j++){
    					gestureValues[6*i + j] = 0.0;   
    				}				
    			}    			
    			
    		}
    		
    		numericalRep.add(gestureValues);
    	}
		
	}
	
	/**
	 * Function used to validate whether or not a user's gesture
	 * matches the training values that they had previously stored
	 * 
	 * @param testGesture Gesture used to authenticate
	 * @return True if gesture is acceptable 
	 */
	public boolean authenticate(Gesture testGesture){
		return false;
	}
}
