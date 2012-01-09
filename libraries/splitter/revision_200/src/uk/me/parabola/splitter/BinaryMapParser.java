package uk.me.parabola.splitter;

import crosby.binary.BinaryParser;
import crosby.binary.Osmformat;

import java.util.List;

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

	@Override
	public void complete() {
		// End of map is sent when all input files are processed.
		// So do nothing.
	}

	// Per-block state for parsing, set when processing the header of a block;
	@Override
	protected void parseDense(Osmformat.DenseNodes nodes) {
		long last_id = 0, last_lat = 0, last_lon = 0;
		int j = 0;
		int maxi = nodes.getIdCount();
		boolean isStartNodeOnly = processor.isStartNodeOnly();
		Node tmp = new Node();
		for (int i=0 ; i < maxi; i++) {
			long lat = nodes.getLat(i)+last_lat; last_lat = lat;
			long lon = nodes.getLon(i)+last_lon; last_lon = lon;
			long id =  nodes.getId(i)+last_id; last_id = id;
			double latf = parseLat(lat), lonf = parseLon(lon);

			if (!isStartNodeOnly) 
				tmp = new Node();
			tmp.set(id, latf, lonf);

			if (!isStartNodeOnly) {
				if (nodes.getKeysValsCount() > 0) {
					while (nodes.getKeysVals(j) != 0) {
						int keyid = nodes.getKeysVals(j++);
						int valid = nodes.getKeysVals(j++);
						tmp.addTag(getStringById(keyid),getStringById(valid));
					}
					j++; // Skip over the '0' delimiter.

				}
			}
			processor.processNode(tmp);
			CountNode(tmp.getId());
		}
	}

	@Override
	protected void parseNodes(List<Osmformat.Node> nodes) {
		if (nodes.size() == 0) return;
		for (Osmformat.Node i : nodes) {
			Node tmp = new Node();
			for (int j=0 ; j < i.getKeysCount(); j++)
				tmp.addTag(getStringById(i.getKeys(j)),getStringById(i.getVals(j)));
			long id = i.getId();
			double latf = parseLat(i.getLat()), lonf = parseLon(i.getLon());

			tmp.set(id, latf, lonf);

			processor.processNode(tmp);
			CountNode(tmp.getId());
		}
	}


	@Override
	protected void parseWays(List<Osmformat.Way> ways) {

		long numways = ways.size();
		if (numways == 0) 
			return;
		if (processor.isStartNodeOnly()) {
			// we just count the ways, so no need to iterate through the list
			if (wayCount > 0) {
				long x = (numways + wayCount) % WAY_STATUS_UPDATE_THRESHOLD;
				// get and report the id that hits the threshold value 
				if (x < numways) {
					x = numways - x;
					Osmformat.Way w = ways.get((int)(x - 1));
					System.out.println(Utils.format(wayCount+x) + " ways processed... id=" + w.getId());
				}
			}
			wayCount += numways;
		}
		else {
			for (Osmformat.Way i : ways) {
				Way tmp = new Way();
				for (int j=0 ; j < i.getKeysCount(); j++)
					tmp.addTag(getStringById(i.getKeys(j)),getStringById(i.getVals(j)));

				long last_id=0;
				for (long j : i.getRefsList()) {
					tmp.addRef(j+last_id);
					last_id = j+last_id;
				}

				long id = i.getId();
				tmp.set(id);

				processor.processWay(tmp);
				countWay(i.getId());
			}
		}
	}



	@Override
	protected void parseRelations(List<Osmformat.Relation> rels) {
		if (rels.size() == 0) return;
		for (Osmformat.Relation i : rels) {
			Relation tmp = new Relation();
			for (int j=0 ; j < i.getKeysCount(); j++)
				tmp.addTag(getStringById(i.getKeys(j)),getStringById(i.getVals(j)));

			long id = i.getId();
			tmp.set(id);

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
					etype = "relation";
				else
					assert false; // TODO; Illegal file?

				tmp.addMember(etype,mid,role);
			}
			processor.processRelation(tmp);
			countRelation(tmp.getId());
		}
	}

	@Override
	public void parse(Osmformat.HeaderBlock block) {

		for (String s : block.getRequiredFeaturesList()) {
			if (s.equals("OsmSchema-V0.6")) continue; // OK.
			if (s.equals("DenseNodes")) continue; // OK.
			throw new UnknownFeatureException(s);
		}

		if (block.hasBbox()) {
			final double multiplier = .000000001;
			double rightf = block.getBbox().getRight() * multiplier;
			double leftf = block.getBbox().getLeft() * multiplier;
			double topf = block.getBbox().getTop() * multiplier;
			double bottomf = block.getBbox().getBottom() * multiplier;

			System.out.println("Bounding box "+leftf+" "+bottomf+" "+rightf+" "+topf);

			Area area = new Area(
					Utils.toMapUnit(bottomf),
					Utils.toMapUnit(leftf),
					Utils.toMapUnit(topf),
					Utils.toMapUnit(rightf));
			processor.boundTag(area);
		}
	}

	private void CountNode(long id) {
		nodeCount++;
		if (nodeCount % NODE_STATUS_UPDATE_THRESHOLD == 0) {
			System.out.println(Utils.format(nodeCount) + " nodes processed... id=" + id);
		}

	}

	private void countWay(long id)  {
		wayCount++;
		if (wayCount % WAY_STATUS_UPDATE_THRESHOLD == 0) {
			System.out.println(Utils.format(wayCount) + " ways processed... id=" + id);
		}
	}

	private void countRelation(long id)  {
		relationCount++;
		if (relationCount % RELATION_STATUS_UPDATE_THRESHOLD == 0) {
			System.out.println(Utils.format(relationCount) + " relations processed... id=" + id);
		}
	}

}
