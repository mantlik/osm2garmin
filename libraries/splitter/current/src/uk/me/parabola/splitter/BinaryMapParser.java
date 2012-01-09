package uk.me.parabola.splitter;

import java.util.List;


import crosby.binary.BinaryParser;
import crosby.binary.Osmformat;

public class BinaryMapParser extends BinaryParser {
	// How many elements to process before displaying a status update
	private static final int NODE_STATUS_UPDATE_THRESHOLD = 10000000;
	private static final int WAY_STATUS_UPDATE_THRESHOLD = 1000000;
	private static final int RELATION_STATUS_UPDATE_THRESHOLD = 100000;

	
	private long nodeCount;
	private long wayCount;
	private long relationCount;	
	
	BinaryMapParser(MapProcessor processor) {
		this.processor = processor;
	}
	MapProcessor processor;
	
		public void complete() {
		  // End of map is sent when all input files are processed.
		  // So do nothing.
		}
		
	// Per-block state for parsing, set when processing the header of a block;
	protected void parseDense(Osmformat.DenseNodes nodes) {
		long last_id = 0, last_lat = 0, last_lon = 0;
		int j = 0; 
		for (int i=0 ; i < nodes.getIdCount(); i++) {
			Node tmp = new Node();
			long lat = nodes.getLat(i)+last_lat; last_lat = lat;
			long lon = nodes.getLon(i)+last_lon; last_lon = lon;
			long id =  nodes.getId(i)+last_id; last_id = id;
            double latf = parseLat(lat), lonf = parseLon(lon);
			tmp = new Node();
			tmp.set((int)id, latf, lonf);
            if (nodes.getKeysValsCount() > 0) {
                while (nodes.getKeysVals(j) != 0) {
                    int keyid = nodes.getKeysVals(j++);
                    int valid = nodes.getKeysVals(j++);
                    tmp.addTag(getStringById(keyid),getStringById(valid));
                }
                j++; // Skip over the '0' delimiter.
            }
			processor.processNode(tmp);
			processNodes(tmp);
		}
	}

	protected void parseNodes(List<Osmformat.Node> nodes) {
		for (Osmformat.Node i : nodes) {
			Node tmp = new Node();
			for (int j=0 ; j < i.getKeysCount(); j++)
				tmp.addTag(getStringById(i.getKeys(j)),getStringById(i.getVals(j)));
			long id = i.getId();
			double latf = parseLat(i.getLat()), lonf = parseLon(i.getLon());

			tmp.set((int)id, latf, lonf);

			processor.processNode(tmp);
			processNodes(tmp);
		}
	}

	
	protected void parseWays(List<Osmformat.Way> ways) {
		for (Osmformat.Way i : ways) {
			Way tmp = new Way();
			for (int j=0 ; j < i.getKeysCount(); j++)
				tmp.addTag(getStringById(i.getKeys(j)),getStringById(i.getVals(j)));

			long last_id=0;
			for (long j : i.getRefsList()) {
				tmp.addRef((int)(j+last_id));
				last_id = j+last_id;
			}

			long id = i.getId();
			tmp.set((int)id);

			processor.processWay(tmp);
			processWays(tmp);
		}
	}
	protected void parseRelations(List<Osmformat.Relation> rels) {
		for (Osmformat.Relation i : rels) {
			Relation tmp = new Relation();
			for (int j=0 ; j < i.getKeysCount(); j++)
				tmp.addTag(getStringById(i.getKeys(j)),getStringById(i.getVals(j)));

			long id = i.getId();
			tmp.set((int)id);
			
			long last_mid=0;
			for (int j =0; j < i.getMemidsCount() ; j++) {
				long mid = last_mid + i.getMemids(j);
				last_mid = mid;
				String role = getStringById(i.getRolesSid(j));
				String etype=null;
				
				if (i.getTypes(j) == Osmformat.Relation.MemberType.NODE)
					etype = "node";
				else if (i.getTypes(j) == Osmformat.Relation.MemberType.WAY)
					etype = "way";
				else if (i.getTypes(j) == Osmformat.Relation.MemberType.RELATION)
					continue;
				else
						assert false; // TODO; Illegal file?

				tmp.addMember(etype,(int)mid,role);
			}
			processor.processRelation(tmp);
			processRelations(tmp);
		}
	}

	public void parse(Osmformat.HeaderBlock block) {
		double multiplier = .000000001;
		double rightf = block.getBbox().getRight() * multiplier;
		double leftf = block.getBbox().getLeft() * multiplier;
		double topf = block.getBbox().getTop() * multiplier;
		double bottomf = block.getBbox().getBottom() * multiplier;

		for (String s : block.getRequiredFeaturesList()) {
            if (s.equals("OsmSchema-V0.6")) continue; // OK.
            if (s.equals("DenseNodes")) continue; // OK.
            throw new Error("File requires unknown feature: " + s);
        }

		System.out.println("Bounding box "+leftf+" "+bottomf+" "+rightf+" "+topf);
		
		Area area = new Area(
				Utils.toMapUnit(bottomf),
				Utils.toMapUnit(leftf),
				Utils.toMapUnit(topf),
				Utils.toMapUnit(rightf));
		processor.boundTag(area);
	}
	

	private void processNodes(Node tmp) {
		nodeCount++;
		if (nodeCount % NODE_STATUS_UPDATE_THRESHOLD == 0) {
			System.out.println(Utils.format(nodeCount) + " nodes processed... id="+tmp.getId());
		}

}

private void processWays(Way tmp)  {
	wayCount++;
	if (wayCount % WAY_STATUS_UPDATE_THRESHOLD == 0) {
		System.out.println(Utils.format(wayCount) + " ways processed... id="+tmp.getId());
	}
}
private void processRelations(Relation tmp)  {
	relationCount++;
	if (relationCount % RELATION_STATUS_UPDATE_THRESHOLD == 0) {
		System.out.println(Utils.format(relationCount) + " ways processed... id="+tmp.getId());
	}
}

}
