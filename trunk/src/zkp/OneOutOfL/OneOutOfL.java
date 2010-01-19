package zkp.OneOutOfL;

import elgamal.Ciphertext;
import elgamal.CryptObject;
import elgamal.ElGamal;
import global.BigIntegerMod;
import java.math.BigInteger;
import global.Consts;
import zkp.ZkpException;
import zkp.OneOutOfL.OneOutOfLTest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class OneOutOfL implements IOneOutOfL {

	public static final boolean TEST = true;
	private ArrayList<Ciphertext> pairslist;

	public OneOutOfL(ArrayList<Ciphertext> pairslist) {
		this.pairslist = pairslist;
	}
	
	public OneOutOfLProof createOneOutOfLProof(CryptObject cryptobj, BigIntegerMod h, int t)
	throws ZkpException 
	{
		int l = pairslist.size(); //or Consts.PARTIES_AMOUNT
		
	
		BigIntegerMod g = Consts.getG();
		
		//for test: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
		//BigIntegerMod g = OneOutOfLTest.g;
		BigInteger q = g.getMod().subtract(BigInteger.ONE).divide(Consts.TWO);
		//BigInteger q = Consts.getQ();
		BigIntegerMod x = cryptobj.getCiphertext().getA();
		BigIntegerMod y = cryptobj.getCiphertext().getB();
		BigIntegerMod r = cryptobj.getR();
		
		if (TEST) {
			System.out.println("testing createOneOutOfLProof:\n");
			System.out.println("CryptObject A: " + x.toString() + "\n");
			System.out.println("CryptObject B: " + y.toString() + "\n");
			System.out.println("Random BigIntegerMod: " + r.toString() + "\n");
			System.out.println("t index: " + t + "\n");
		}
			
		//check t - index parameter for re-encrypted pair 
		if (t<0 || t>l)
			throw new ZkpException("index input t is <0 or >l");
		
		// check whether cryptobj is indeed a re-encryption using r of the pair in index t
		//CryptObject test = new CryptObject();
		//ElGamal gamal=new ElGamal(OneOutOfLTest.p,g,h,null);
		//ElGamal gamal=new ElGamal(h);
		//test = gamal.encrypt(pairslist.get(t).getB(), cryptobj.getR());
		//test = gamal.reencrypt(pairslist.get(t), cryptobj.getR());
		//if ((test.getCiphertext().getA().compareTo(cryptobj.getCiphertext().getA()) !=0) ||
		//	(test.getCiphertext().getB().compareTo(cryptobj.getCiphertext().getB()) !=0))
		if ((!(pairslist.get(t).getA().equals(cryptobj.getCiphertext().getA().multiply(g.pow(cryptobj.getR()))))) && 
			(!(pairslist.get(t).getB().equals(cryptobj.getCiphertext().getB().multiply(h.pow(cryptobj.getR()))))))
		//if ((!(test.getCiphertext().getA().equals(cryptobj.getCiphertext().getA().multiply(g.pow(cryptobj.getR()))))) && 
		//	(!(test.getCiphertext().getB().equals(cryptobj.getCiphertext().getB().multiply(h.pow(cryptobj.getR()))))))
			throw new ZkpException("input CryptObject is not a re-encryption of the CryptObject in index t of the list");
		
		
		// randomly select d_List and r_List with Zq numbers
		ArrayList<BigIntegerMod> d_List = new ArrayList<BigIntegerMod>();
		ArrayList<BigIntegerMod> r_List = new ArrayList<BigIntegerMod>();
		
		for (int i=0; i<l; i++) 
		{
			//d_List.add(new BigIntegerMod(Util.createRandom(q), q));
			//r_List.add(new BigIntegerMod(Util.createRandom(q), q));
			d_List.add(new BigIntegerMod(q));
			r_List.add(new BigIntegerMod(q));
		}
		
		/*
		d_List.add(new BigIntegerMod(new BigInteger("1"), q));
		d_List.add(new BigIntegerMod(new BigInteger("10"), q));
		d_List.add(new BigIntegerMod(new BigInteger("9"), q));
		r_List.add(new BigIntegerMod(new BigInteger("5"), q));
		r_List.add(new BigIntegerMod(new BigInteger("0"), q));
		r_List.add(new BigIntegerMod(new BigInteger("0"), q));
		*/
		
		if (TEST) {
			System.out.println("d_List: " + d_List.toString() + "\n");
			System.out.println("r_List: " + r_List.toString() + "\n");
		}
		
		// create a_List and b_list
		ArrayList<BigIntegerMod> a_List = new ArrayList<BigIntegerMod>();
		ArrayList<BigIntegerMod> b_List = new ArrayList<BigIntegerMod>();
		
		// let ai=((xi/x)^di)*g^ri and bi=((yi/y)^di)*h^ri
		for (int i=0; i<l; i++) {	
			BigIntegerMod x_i = pairslist.get(i).getA();
			BigIntegerMod y_i = pairslist.get(i).getB();
			BigIntegerMod d_i = d_List.get(i);
			BigIntegerMod r_i = r_List.get(i);
			BigIntegerMod a_i = null;
			BigIntegerMod b_i = null;
				
			try{
				a_i = (((x_i.multiply(x.inverse()))).pow(d_i)).multiply(g.pow(r_i));
				b_i = (((y_i.multiply(y.inverse()))).pow(d_i)).multiply(h.pow(r_i));
			}
			catch (ArithmeticException exception){
				throw new ZkpException(exception.getMessage());
			}			
			a_List.add(a_i);
			b_List.add(b_i);
			
		}
		
		if (TEST) {
			System.out.println("a_List: " + a_List.toString() + "\n");
			System.out.println("b_List: " + b_List.toString() + "\n");
		}
		
		// compute the challenge using md5 hash function with x,y,a_List,b_list
		BigIntegerMod c = new BigIntegerMod(createOneOutOfLHashChallenge(x,y,a_List,b_List,q), q);
				
		// compute w=(r*d_List[t])+r_List[t]
		//r = r.multiply(new BigIntegerMod(new BigInteger("-1"),q));
		BigIntegerMod w = (r.multiply(d_List.get(t))).add(r_List.get(t));
		
		// change d_List[t]=c-sum(d_List[j!=t])
		BigIntegerMod sumD_List = d_List.get(0);
		for (int j=1; j<l; j++)
			sumD_List = sumD_List.add(d_List.get(j));
		sumD_List = sumD_List.substract(d_List.get(t));
		d_List.set(t,(c.substract(sumD_List)));
		
		// change r_List[t]=w-r*d_List[t]
		r_List.set(t,(w.substract(r.multiply(d_List.get(t)))));
		
		if (TEST) {
			System.out.println("hash challenge: " + c.toString() + "\n");
			System.out.println("w: " + w.toString() + "\n");
			System.out.println("new d_List: " + d_List.toString() + "\n");
			System.out.println("new r_List: " + r_List.toString() + "\n");
		}
				
		return new OneOutOfLProof(c, d_List, r_List);
	}

	public boolean verifyOneOutOfLProof(OneOutOfLProof proof, Ciphertext cipher, BigIntegerMod h) 
			throws ZkpException 
	{
		int l = pairslist.size(); //or Consts.PARTIES_AMOUNT????????
		
		BigIntegerMod g = Consts.getG();
		//for test: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
		//BigIntegerMod g = OneOutOfLTest.g;
		BigInteger q = g.getMod().subtract(BigInteger.ONE).divide(Consts.TWO);
		//BigInteger q = Consts.getQ();
		BigIntegerMod c = proof.getC();
		ArrayList<BigIntegerMod> d_List = proof.getD_List();
		ArrayList<BigIntegerMod> r_List = proof.getR_List();
		
		BigIntegerMod x = cipher.getA();
		BigIntegerMod y = cipher.getB();
		
		if (TEST) {
			System.out.println("testing verifyOneOutOfLProof:\n");
			System.out.println("CryptObject A: " + x.toString() + "\n");
			System.out.println("CryptObject B: " + y.toString() + "\n");
			System.out.println("proof - hash challenge: "+proof.getC().toString()+"\n");
			System.out.println("proof - d_List: "+proof.getD_List().toString()+"\n");
			System.out.println("proof - r_List: "+proof.getR_List().toString()+"\n");
		}
		
		// Check whether c=sum(d_List[j])
		BigIntegerMod sumD_List = d_List.get(0);
		for (int j=1; j<l; j++)
			sumD_List = sumD_List.add(d_List.get(j));
		if (sumD_List.compareTo(c) != 0)
			return false;
		
		// create a_List and b_list
		ArrayList<BigIntegerMod> a_List = new ArrayList<BigIntegerMod>();
		ArrayList<BigIntegerMod> b_List = new ArrayList<BigIntegerMod>();
		
		// let ai=((xi/x)^di)*g^ri and bi=((yi/y)^di)*h^ri
		for (int i=0; i<l; i++) {	
			BigIntegerMod x_i = pairslist.get(i).getA();
			BigIntegerMod y_i = pairslist.get(i).getB();
			BigIntegerMod d_i = d_List.get(i);
			BigIntegerMod r_i = r_List.get(i);
			BigIntegerMod a_i = null;
			BigIntegerMod b_i = null;
				
			try{
				a_i = (((x_i.multiply(x.inverse()))).pow(d_i)).multiply(g.pow(r_i));
				b_i = (((y_i.multiply(y.inverse()))).pow(d_i)).multiply(h.pow(r_i));
			}
			catch (ArithmeticException exception){
				throw new ZkpException(exception.getMessage());
			}			
			a_List.add(a_i);
			b_List.add(b_i);
		}
		
		if (TEST) {
			System.out.println("a_List: " + a_List.toString() + "\n");
			System.out.println("b_List: " + b_List.toString() + "\n");
		}
				
		// compute the challenge using md5 hash function with x,y,a_List,b_list
		BigIntegerMod new_c = new BigIntegerMod(createOneOutOfLHashChallenge(x,y,a_List,b_List,q), q);
		
		if (TEST) {
			System.out.println("new hash challenge: " + new_c.toString() + "\n");
			if (new_c.compareTo(c) == 0)
				System.out.println("CONGRATULATIONS! ZKP ONE OUT OF L PROOF RETURNED TRUE\n");
			else
				System.out.println("THE HORROR! ZKP ONE OUT OF L PROOF RETURNED FALSE\n");
		}
		
		if (new_c.compareTo(c) == 0)
			return true;
		else
			return false;
	}
	
	/**
	 * Creates a challenge for One Out Of L function, using a MD5 hash function
	 * @param x - first BigIntegerMod in the Ciphertext pair
	 * @param y - second BigIntegerMod in the Ciphertext pair
	 * @param a_List - list of BigIntegerMod items created by createOneOutOfLProof function
	 * @param b_List - list of BigIntegerMod items created by createOneOutOfLProof function
	 * @param modulo - the modulo in use 
	 * @return BigInteger object
	 * @throws ZkpException
	 */
	
	private static BigInteger createOneOutOfLHashChallenge(BigIntegerMod x, BigIntegerMod y, 
			ArrayList<BigIntegerMod> a_List, ArrayList<BigIntegerMod> b_List, BigInteger modulo) throws ZkpException
	{

		BigInteger challenge = null;
		//use MD5 hash function for challenge
		MessageDigest m;
		try
		{
			m = MessageDigest.getInstance("MD5");
			m.update(x.getValue().toByteArray());
			m.update(y.getValue().toByteArray());
			for (BigIntegerMod a_i : a_List)
				m.update(a_i.getValue().toByteArray());
			for (BigIntegerMod b_i : b_List)
				m.update(b_i.getValue().toByteArray());
			challenge = new BigInteger(m.digest());
		}
		catch (NoSuchAlgorithmException exception)
		{
			throw new ZkpException(exception.getMessage());
		}

		return challenge.mod(modulo);
	}
	
		
}

