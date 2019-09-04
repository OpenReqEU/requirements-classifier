package com.example.mahout;

import com.example.mahout.entity.Requirement;
import com.example.mahout.util.Control;
import org.codehaus.jettison.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class SamplesCreator {

	static int [] values;
	static int n;

	private SamplesCreator() {
		//utility class
	}

	public static Map<String, List<List<Requirement>>> generateTestSets(List<Requirement> requirements, int number) throws JSONException {
		Map<String, List<List<Requirement>>> map = new HashMap<>();
 		n = max(number,2);
		int iters = min(n, number);
		values = new int[n];
		boolean onlyOne = (number == 1);

		/* Initialize structures to Write into test sets*/
		List<List<Requirement>> testSetReqs = new ArrayList<>();
		for (int i = 0; i < iters; i++) {
			List<Requirement> reqs = new ArrayList<>();
			testSetReqs.add(reqs);
		}

		/* Initialize structures to read training sets */
		List<List<Requirement>> trainSetReqs = new ArrayList<>();
		for (int i = 0; i < iters; i++) {
            List<Requirement> reqs = new ArrayList<>();
			trainSetReqs.add(reqs);
		}

		int numero = 0;
		for (int i = 0; i < requirements.size(); i++) {
			Requirement req = requirements.get(i);
			double d = ThreadLocalRandom.current().nextDouble(0,n);
			numero = (int) (d + 1);
			boolean flag = true;
			while (flag) {
				if (validate(numero)) {
					if (onlyOne) {
						if (numero == 1)
							testSetReqs.get(numero - 1).add(req);
						else
							trainSetReqs.get(0).add(req);
					} else {
						testSetReqs.get(numero - 1).add(req);
					}
					flag = false;
				} else {
					double d2 = ThreadLocalRandom.current().nextDouble(0, n);
					numero = (int) (d2 + 1);
				}
				validateFor();
			}

		}

		if (!onlyOne) {
            for (int a = 0; a < n; a++) {
                List<Requirement> reqs = testSetReqs.get(a);
                for (int i = 0; i < reqs.size(); ++i) {
                    Requirement req = reqs.get(i);
                    for (int c = 0; c < n; c++) {
                        if (c != a) {
                            trainSetReqs.get(c).add(req);
                        }
                    }
                }
            }
        }

		map.put("train_sets", trainSetReqs);
		map.put("test_sets", testSetReqs);

		Control.getInstance().showInfoMessage("Finished");
		for (int i = 0; i < testSetReqs.size(); i++)
			Control.getInstance().showInfoMessage("Requirement array " + i + " size: " + testSetReqs.get(i).size());
		return map;
	}

	static void fill() {
		for(int x=0;x<n;x++)
			values[x]=x+1;

	}

	static boolean validate(int x) {
		return values[x-1]!=0;
	}

	static void validateFor() {
		int flag = 0;

		for(int x=0;x<n;x++) {
			if(values[x]==0) {
				flag++;
			}
		}

		if(flag==n) {
			fill();
		}
	}
}
