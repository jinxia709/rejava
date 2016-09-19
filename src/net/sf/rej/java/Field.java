/* Copyright (C) 2004-2007 Sami Koivu
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.sf.rej.java;

import java.util.HashMap;
import java.util.Map;

import net.sf.rej.java.attribute.Attributes;
import net.sf.rej.java.attribute.ConstantValueAttribute;
import net.sf.rej.java.constantpool.ConstantPool;
import net.sf.rej.java.constantpool.ConstantPoolInfo;
import net.sf.rej.java.constantpool.DescriptorEnabled;
import net.sf.rej.java.constantpool.UTF8Info;
import net.sf.rej.util.ByteParser;
import net.sf.rej.util.ByteSerializer;
import net.sf.rej.util.ByteToolkit;
import net.sf.rej.util.Range;

public class Field implements DescriptorEnabled {

    private int accessFlags;
    private int nameIndex;
    private int descriptorIndex;

    private ConstantPool pool;

    private Attributes attributes;

    protected Field(ConstantPool pool) {
    	this.pool = pool;
    	this.attributes = new Attributes();
    }

    protected Field(ByteParser parser, ConstantPool pool) {
        this.pool = pool;
        this.accessFlags = parser.getShortAsInt();
        this.nameIndex = parser.getShortAsInt();
        this.descriptorIndex = parser.getShortAsInt();
        this.attributes = new Attributes(parser, this.pool);
    }

    public int getAccessFlags() {
        return this.accessFlags;
    }

    public int getNameIndex() {
        return this.nameIndex;
    }

    public int getDescriptorIndex() {
        return this.descriptorIndex;
    }

    public Attributes getAttributes() {
        return this.attributes;
    }

    @Override
	public String toString() {
        return "Field: nameIndex " + this.nameIndex + "(" + this.pool.get(this.nameIndex)
                + ") desriptor index " + this.descriptorIndex + "("
                + this.pool.get(this.descriptorIndex) + ") accessflags 0x"
                + ByteToolkit.getHexString(this.accessFlags, 4);
    }

    public String getName() {
        UTF8Info info = (UTF8Info) this.pool.get(this.nameIndex);

        return info.getValue();
    }

    public Descriptor getDescriptor() {
        UTF8Info info = (UTF8Info) this.pool.get(this.descriptorIndex);
        return new Descriptor(info.getValue());
    }

    public String getSignatureLine() {
        return getSignatureLine(null);
    }

    public String getSignatureLine(String className) {
        StringBuffer sb = new StringBuffer();
        JavaType ret = getDescriptor().getReturn();
        if (ret.isPrimitive()) {
            sb.append(ret);
        } else {
            sb.append(ret.getType());
            sb.append(ret.getDimensions());
        }
        sb.append(" ");
        if(className != null) {
            sb.append(className);
            sb.append(".");
        }
        sb.append(getName());
        return sb.toString();
    }

    public String getAccessString() {
        StringBuffer sb = new StringBuffer();
        if (AccessFlags.isPublic(this.accessFlags))
            sb.append("public ");
        if (AccessFlags.isPrivate(this.accessFlags))
            sb.append("private ");
        if (AccessFlags.isProtected(this.accessFlags))
            sb.append("protected ");
        if (AccessFlags.isAbstract(this.accessFlags))
            sb.append("abstract ");
        if (AccessFlags.isStatic(this.accessFlags))
            sb.append("static ");
        if (AccessFlags.isSynchronized(this.accessFlags))
            sb.append("synchronized ");
        if (AccessFlags.isFinal(this.accessFlags))
            sb.append("final ");
        if (AccessFlags.isNative(this.accessFlags))
            sb.append("native ");

        return sb.toString().trim();
    }

    public void setAccessFlags(AccessFlags accessFlags) {
        this.accessFlags = accessFlags.getValue();
    }

	public void setDescriptorIndex(int descriptorIndex) {
		this.descriptorIndex = descriptorIndex;
	}

	public void setNameIndex(int nameIndex) {
		this.nameIndex = nameIndex;
	}
	
	public ConstantPoolInfo getConstant() {
		ConstantValueAttribute attr = this.attributes.getConstantValueAttribute();
		if (attr != null) {
			int index = attr.getConstantIndex();
			ConstantPoolInfo cpi = this.pool.get(index);
			return cpi;
		}
		
		return null;
	}

    public byte[] getData() {
        ByteSerializer ser = new ByteSerializer(true);
        ser.addShort(this.accessFlags);
        ser.addShort(this.nameIndex);
        ser.addShort(this.descriptorIndex);
        ser.addBytes(this.attributes.getData());

        return ser.getBytes();
    }

    public static enum OffsetTag {ACCESS_FLAGS, FIELD_NAME, FIELD_DESCRIPTOR, ATTRIBUTES}
    
    /**
     * Returns a map of offsets of each significant element of this field.
     * The offsets returned by this method are only valid until this
     * object is modified. The keys in the map are
     * of type <code>OffsetTag</code>, <code>Attribute</code>. 
     * 
     * @return a map of element offsets in class file data.
     */
    public Map<Object, Range> getOffsetMap() {
    	Map<Object, Range> map = new HashMap<Object, Range>();
    	int offset = 0;
    	map.put(OffsetTag.ACCESS_FLAGS, new Range(offset, 2));
    	offset += 2;
    	map.put(OffsetTag.FIELD_NAME, new Range(offset, 2));
    	offset += 2;
    	map.put(OffsetTag.FIELD_DESCRIPTOR, new Range(offset, 2));
    	offset += 2;
    	map.put(OffsetTag.ATTRIBUTES, new Range(offset, this.attributes.getData().length));
    	// each attribute
    	
    	map.putAll(this.attributes.getOffsetMap(offset));

    	return map;
    }

}
