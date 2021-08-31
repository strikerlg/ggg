/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta.schema.views;

import com.axelor.i18n.I18n;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class MenuItem extends SimpleWidget {

  @XmlAttribute(name = "id")
  private String xmlId;

  @XmlAttribute private String parent;

  @XmlAttribute private String icon;

  @XmlAttribute(name = "icon-background")
  private String iconBackground;

  @XmlAttribute private String action;

  @XmlAttribute private String prompt;

  @XmlAttribute private Integer order;

  @XmlAttribute private String groups;

  @XmlAttribute private Boolean top;

  @XmlAttribute private Boolean left;

  @XmlAttribute private Boolean mobile;

  @XmlAttribute private String category;

  @XmlAttribute private String tag;

  @XmlAttribute(name = "tag-get")
  private String tagGet;

  @XmlAttribute(name = "tag-count")
  private Boolean tagCount;

  @XmlAttribute(name = "tag-style")
  private String tagStyle;

  @XmlAttribute(name = "has-tag")
  private Boolean hasTag;

  public String getXmlId() {
    return xmlId;
  }

  public void setXmlId(String xmlId) {
    this.xmlId = xmlId;
  }

  public String getParent() {
    return parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public Integer getOrder() {
    return order;
  }

  public void setOrder(Integer order) {
    this.order = order;
  }

  public String getGroups() {
    return groups;
  }

  public void setGroups(String groups) {
    this.groups = groups;
  }

  public Boolean getTop() {
    return top;
  }

  public void setTop(Boolean top) {
    this.top = top;
  }

  public Boolean getLeft() {
    return left;
  }

  public void setLeft(Boolean left) {
    this.left = left;
  }

  public Boolean getMobile() {
    return mobile;
  }

  public void setMobile(Boolean mobile) {
    this.mobile = mobile;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getIconBackground() {
    return iconBackground;
  }

  public void setIconBackground(String iconBackground) {
    this.iconBackground = iconBackground;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  @JsonGetter("prompt")
  public String getLocalizedPrompt() {
    return I18n.get(prompt);
  }

  @JsonIgnore
  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  @JsonIgnore
  public String getTag() {
    return tag;
  }

  @JsonGetter("tag")
  public String getLocalizedTag() {
    return I18n.get(tag);
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getTagGet() {
    return tagGet;
  }

  public void setTagGet(String tagGet) {
    this.tagGet = tagGet;
  }

  public Boolean getTagCount() {
    return tagCount;
  }

  public void setTagCount(Boolean tagCount) {
    this.tagCount = tagCount;
  }

  public String getTagStyle() {
    return tagStyle;
  }

  public void setTagStyle(String tagStyle) {
    this.tagStyle = tagStyle;
  }

  public Boolean getHasTag() {
    return hasTag;
  }

  public void setHasTag(Boolean hasTag) {
    this.hasTag = hasTag;
  }
}
