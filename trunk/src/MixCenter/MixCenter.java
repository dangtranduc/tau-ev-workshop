package MixCenter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import sun.awt.datatransfer.DataTransferer.ReencodingInputStream;
import elgamal.CryptObject;
import global.Consts;
import global.Consts.DebugOutput;
import elgamal.ElGamal;
import global.BigIntegerMod;

public class MixCenter 
{
	private static BufferedWriter outputFile = null;
	private static final String MC_RESULTS_FILE = "Mix_Center_Log.txt";
	
	/*
	 * generates the array (pi) that represents the permutation that will be made.
	 */
	private int[] generatePermutation()
	{
		int d,temp;
		int n=Consts.VOTERS_AMOUNT;
		int[] result = new int[n]; //pi
		Random generator = new Random();
		for(int i=0;i<n;i++) //initialize the permutation array.
		{
			result[i]=i;
		}
		for(int i=0;i<n-1;i++)//for all cells except the last one [0,n-2]
		{
			d=generator.nextInt(n-i)+i; //generates a random number [i,n-1]
			temp=result[i];
			result[i]=result[d];
			result[d]=temp;			
		}
		return result;
	}
	/*
	 * permutate and re-encrypt A according to pi and using ElGamal module.
	 */
	public CryptObject[] PermutateAndReecncrypt(CryptObject[] A, int[] pi)
	{
		ElGamal gamal=new ElGamal(publicKey);
		int n=Consts.VOTERS_AMOUNT;
		CryptObject[] B=new CryptObject[n];	
		for(int i=0;i<n;i++) //create permutation according to pi[] and then - re-encrypt
		{
			B[i]=gamal.reencrypt(A[pi[i]]);			
		}
		return B;
	}
	
	/*
	 * Print results to file
	 * Params: message - string to print
	 *         A - encrypted votes array before re-encryption and mixing
	 *         B - re-encrypted and mixed votes array
	 */
	public static void printToFile(String message, CryptObject[] A, CryptObject[] B)
	{
		int n=Consts.VOTERS_AMOUNT;	//TODO: make it a field so we wont read it all the time?	
		
		try 
		{
			if (outputFile == null)
			{
				outputFile = new BufferedWriter(new FileWriter(MC_RESULTS_FILE));
			}
			// print ZKP string
			outputFile.write(message + "\r\n");
			
			// print A array
			outputFile.write(" A: [ ");
			for (int i=0; i<n; i++)
			{
				outputFile.write(A[i].getText().getValue().toString() + " ");
			}
			outputFile.write("]\n");
			
			// print B array
			outputFile.write(" B: [ ");
			for (int i=0; i<n; i++)
			{
				outputFile.write(B[i].getText().getValue().toString() + " ");
			}
			outputFile.write("]\n");
			
			outputFile.flush(); // TODO: is it possible buffer will be full  sooner?
		}
		catch (IOException e) 
		{
			System.err.println(e);
		}	
	}
	
	/*
	 * Performs Zero Knowledge Proof
	 * Params: A - encrypted votes array before re-encryption and mixing
	 *         B - re-encrypted and mixed votes array
	 *         pi - new permutation array
	 *         R - random numbers arrays, whichh were used for re-encrypting (according to the original permutation???)
	 */
	private void performZKP(CryptObject[] A, CryptObject[] B, int[] pi) 
	{
		String sZKP = " ";
		int n=Consts.VOTERS_AMOUNT;	//TODO: make it a field so we wont read it all the time?	
		BigIntegerMod[] R = new BigIntegerMod[n];
		// get W (the publicKey)
		
		// prepare R
		for (int i=0; i<n; i++)
		{
			R[i] = B[i].getR();
		}
		
		/* call ZKP function  
		sZKP = verifyGIProof(A, B, pi, R, W);*/
		
		printToFile(sZKP, A, B);
				
	}
	
	

}
