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
(function() {

"use strict";

var ui = angular.module('axelor.ui');

/**
 * The Boolean input widget.
 */
ui.formInput('Boolean', {

  css: 'boolean-item',

  cellCss: 'form-item boolean-item',

  link: function (scope, element, attrs, model) {

    element.on('click', 'input:not(.no-toggle)', function (e) {
      var doClick = function () {
        var value = $(e.target).data('value');
        var checked = value === undefined ? e.target.checked : value;
        if (scope.field.nullable && value === model.$viewValue ) {
          $(e.target).prop('checked', false);
          checked = null;
        }
        scope.setValue(checked, true);
      };

      if (scope.waitForActions) {
        scope.waitForActions(doClick);
      } else {
        doClick();
      }
    });

    Object.defineProperty(scope, '$value', {
      get: function () {
        return model.$viewValue;
      },
      set: function(value) {
        model.$setViewValue(value);
      }
    });
  },
  template_readonly: null,
  template_editable:
    "<label class='ibox'>" +
      "<input type='checkbox' ng-model='$value' ng-disabled='isReadonly()'>" +
      "<span class='box'></span>" +
    "</label>"
});

/**
 * The Boolean widget with label on right.
 */
ui.formInput('InlineCheckbox', 'Boolean', {
  css: 'checkbox-inline',
  metaWidget: true,
  showTitle: false,
  link: function (scope, element, attrs, model) {
    this._super.apply(this, arguments);
    scope.$watch('attr("title")', function booleanTitleWatch(title) {
      scope.label = title;
    });
  },
  template_readonly: null,
  template_editable:
    "<label class='ibox'>" +
      "<input type='checkbox' ng-model='$value' ng-disabled='isReadonly()'>" +
      "<div class='box'></div>" +
      "<span class='title' ui-help-popover>{{label}}</span>" +
    "</label>"
});

ui.formInput('Toggle', 'Boolean', {
  cellCss: 'form-item toggle-item',
  metaWidget: true,
  link: function (scope, element, attrs, model) {
    this._super.apply(this, arguments);

    var field = scope.field;
    var icon = element.find('i');

    scope.icon = function () {
      return model.$viewValue && field.iconActive ? field.iconActive : field.icon;
    };

    scope.toggle = function () {
      var value = !model.$viewValue;
      if (scope.setExclusive && field.exclusive) {
        scope.setExclusive(field.name, scope.record);
      }
      scope.setValue(value, true);
    };

    if (field.help || field.title) {
      element.attr('title', field.help || field.title);
    }
  },
  template_readonly: null,
  template_editable:
    "<button tabindex='-1' class='btn btn-default' ng-class='{active: $value}' ng-click='toggle()'>" +
      "<i class='fa {{icon()}}'></i>" +
    "</button>"
});

ui.formInput('BooleanSelect', 'Boolean', {
  css: 'form-item boolean-select-item',
  metaWidget: true,
  init: function (scope) {
    var field = scope.field;
    var trueText = _t((field.widgetAttrs||{}).trueText) || _t('Yes');
    var falseText = _t((field.widgetAttrs||{}).falseText) || _t('No');

    scope.$items = [trueText, falseText];
    scope.$selection = [{ value: trueText, val: true}, { value: falseText, val: false }];
    if (field.nullable) {
      scope.$selection.unshift({ value: '', val: null });
    }
    scope.format = function (value) {
      if (field.nullable && (value === null || value === undefined)) {
        return "";
      }
      return value ? scope.$items[0] : scope.$items[1];
    };
  },
  link_editable: function (scope, element, attrs, model) {
    var input = element.find('input');
    var items = scope.$items;

    input.autocomplete({
      minLength: 0,
      source: scope.$selection,
      select: function (e, u) {
        scope.setValue(u.item.val, true);
        scope.$applyAsync();
      }
    }).click(function (e) {
      input.autocomplete("search" , '');
    });

    scope.doShowSelect = function () {
      input.autocomplete("search" , '');
    };

    scope.$render_editable = function () {
      var value = model.$viewValue;
      var text = scope.format(value);
      input.val(text);
    };

    scope.$watch('isReadonly()', function booleanReadonlyWatch(readonly) {
      input.autocomplete(readonly ? "disable" : "enable");
      input.toggleClass('not-readonly', !readonly);
    });
  },
  template: "<span class='form-item-container'></span>",
  template_readonly: '<span>{{text}}</span>',
  template_editable: "<span class='picker-input'>" +
        "<input type='text' readonly='readonly' class='no-toggle'>" +
        "<span class='picker-icons picker-icons-1'>" +
          "<i class='fa fa-caret-down' ng-click='doShowSelect()'></i>" +
        "</span>" +
      "</span>"
});

ui.formInput('BooleanRadio', 'BooleanSelect', {
  css: 'form-item boolean-radio-item',
  metaWidget: true,
  link_editable: function (scope, element, attrs, model) {
    var field = scope.field;

    if (field.direction === "vertical") {
      element.addClass("boolean-radio-vertical");
    }

    var inputName = _.uniqueId('boolean-radio');
    var trueInput = $('<input type="radio" data-value="true" name="' + inputName + '">');
    var falseInput = $('<input type="radio" data-value="false" name="' + inputName + '">');

    var items = scope.$items;

    $('<li>').append(
      $('<label class="ibox round">')
      .append(trueInput)
      .append($('<i class="box">'))
      .append($('<span class="title">').text(items[0]))
    ).appendTo(element);

    $('<li>').append(
      $('<label class="ibox round">')
      .append(falseInput)
      .append($('<i class="box">'))
      .append($('<span class="title">').text(items[1]))
    ).appendTo(element);

    scope.$render_editable = function () {
      var value = scope.field.nullable ? model.$viewValue : model.$viewValue || false;
      if (value) {
        trueInput.prop('checked', true);
      } else if (value == false) {
        falseInput.prop('checked', true);
      }
    };
  },
  template_editable: "<ul class='boolean-radio'></ul>"
});

ui.formInput('BooleanSwitch', 'Boolean', {
  css: 'form-item',
  metaWidget: true,
  template_readonly: null,
  template_editable:
    "<label class='iswitch'>" +
      "<input type='checkbox' ng-model='$value' ng-disabled='isReadonly()'>" +
      "<span class='box'></span>" +
    "</label>"
});

})();
