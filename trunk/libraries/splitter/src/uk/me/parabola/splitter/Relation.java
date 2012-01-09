/*
 * Copyright (c) 2009.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */
package uk.me.parabola.splitter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ratcliffe
 */
public class Relation extends Element {
	private final List<Member> members = new ArrayList<Member>();

	public void set(int id) {
		setId(id);
	}

	@Override
	public void reset() {
		super.reset();
		members.clear();
	}

	public void addMember(String type, int ref, String role) {
		Member mem = new Member(type, ref, role);
		members.add(mem);
	}

	public List<Member> getMembers() {
		return members;
	}

	static class Member {
		private String type;
		private int ref;
		private String role;

		Member(String type, int ref, String role) {
			this.type = type;
			this.ref = ref;
			this.role = role;
		}

		public String getType() {
			return type;
		}

		public int getRef() {
			return ref;
		}

		public String getRole() {
			return role;
		}
	}
}
