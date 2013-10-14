package poke.client.util;

import poke.util.PrintNode;
import eye.Comm.Document;
import eye.Comm.Finger;
import eye.Comm.Header;
import eye.Comm.NameValueSet;

public class ClientUtil {

	public static void printDocument(Document doc) {
		if (doc == null) {
			System.out.println("document is null");
			return;
		}

		if (doc.hasNameSpace())
			System.out.println("NameSpace: " + doc.getNameSpace());

		if (doc.hasDocument()) {
			NameValueSet nvs = doc.getDocument();
			PrintNode.print(nvs);
		}
	}

	public static void printFinger(Finger f) {
		if (f == null) {
			System.out.println("finger is null");
			return;
		}

		System.out.println("Poke: " + f.getTag() + " - " + f.getNumber());
	}

	public static void printHeader(Header h) {
		System.out.println("-------------------------------------------------------");
		System.out.println("Header");
		System.out.println(" - Orig   : " + h.getOriginator());
		System.out.println(" - Req ID : " + h.getRoutingId());
		System.out.println(" - Tag    : " + h.getTag());
		System.out.println(" - Time   : " + h.getTime());
		System.out.println(" - Status : " + h.getReplyCode());
		if (h.getReplyCode().getNumber() != eye.Comm.Header.ReplyStatus.SUCCESS_VALUE)
			System.out.println(" - Re Msg : " + h.getReplyMsg());

		System.out.println("");
	}

}
