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

'use strict';

var ui = angular.module('axelor.ui');

var OPERATORS = {
  "="		: _t("equals"),
  "!="	: _t("not equal"),
  ">="	: _t("greater or equal"),
  "<="	: _t("less or equal"),
  ">" 	: _t("greater than"),
  "<" 	: _t("less than"),

  "like" 		: _t("contains"),
  "notLike"	: _t("doesn't contain"),

  "in" 		: _t("in"),
  "notIn"	: _t("not in"),

  "between"		: _t("in range"),
  "notBetween"	: _t("not in range"),

  "isNull"	: _t("is null"),
  "notNull" 	: _t("is not null"),

  "true"		: _t("is true"),
  "false" 	: _t("is false"),

  "$inPast": _t("in the past"),
  "$inNext": _t("in the next"),
  "$inCurrent": _t("in the current"),

  "$isCurrentUser": _t("is current user"),
  "$isCurrentGroup": _t("is current group")
};

var OPERATORS_BY_TYPE = {
  "enum"		: ["=", "!=", "isNull", "notNull"],
  "text"		: ["like", "notLike", "isNull", "notNull"],
  "string"	: ["=", "!=", "like", "notLike", "isNull", "notNull"],
  "integer"	: ["=", "!=", ">=", "<=", ">", "<", "between", "notBetween", "isNull", "notNull"],
  "boolean"	: ["true", "false"]
};

_.each(["long", "decimal", "time"], function(type) {
  OPERATORS_BY_TYPE[type] = OPERATORS_BY_TYPE.integer;
});

var DATE_OPERATORS = OPERATORS_BY_TYPE.integer.concat(["$inPast", "$inNext", "$inCurrent"]);

_.each(["date", "datetime"], function(type) {
  OPERATORS_BY_TYPE[type] = DATE_OPERATORS;
});

_.each(["one-to-many"], function(type) {
  OPERATORS_BY_TYPE[type] = ["isNull", "notNull"];
});

_.each(["one-to-one", "many-to-one", "many-to-many"], function(type) {
  OPERATORS_BY_TYPE[type] = ["like", "notLike", "in", "notIn", "isNull", "notNull"];
});

var EXTRA_OPERATORS_BY_TARGET = {
  "com.axelor.auth.db.User": ["$isCurrentUser"],
  "com.axelor.auth.db.Group": ["$isCurrentGroup"]
};

var CAN_SHOW = {
  "$inPast": { timeUnit: true },
  "$inNext": { timeUnit: true },
  "$inCurrent": { input: false, timeUnit: true },
  "$isCurrentUser": { input: false },
  "$isCurrentGroup": { input: false }
};

var PAST_NEXT_TIME_UNIT_SELECTION = [
    {value: "day", title: _t("days")},
    {value: "week", title: _t("weeks")},
    {value: "month", title: _t("months")},
    {value: "quarter", title: _t("quarters")},
    {value: "year", title: _t("years")}
];
var CURRENT_TIME_UNIT_SELECTION = [
    {value: "day", title: _t("day")},
    {value: "week", title: _t("week")},
    {value: "month", title: _t("month")},
    {value: "quarter", title: _t("quarter")},
    {value: "year", title: _t("year")}
];

var TIME_UNIT_SELECTIONS = {
  "$inPast": PAST_NEXT_TIME_UNIT_SELECTION,
  "$inNext": PAST_NEXT_TIME_UNIT_SELECTION,
  "$inCurrent": CURRENT_TIME_UNIT_SELECTION
};

var CRITERION_PREPARATORS = {
  "$inPast": function (criterion, filter) {
    criterion.value = criterion.value || 0;
    criterion.timeUnit = filter.timeUnit;
  },
  "$inNext": function (criterion, filter) {
    criterion.value = criterion.value || 0;
    criterion.timeUnit = filter.timeUnit;
  },
  "$inCurrent": function (criterion, filter) {
    criterion.timeUnit = filter.timeUnit;
  },
  "$isCurrentUser": function (criterion) {
    criterion.fieldName += ".id";
  },
  "$isCurrentGroup": function (criterion) {
    criterion.fieldName += ".code";
  }
};

var FILTER_TRANSFORMERS = {
  "$inPast": function (filter) {
    var now = moment().locale(ui.getBrowserLocale());
    filter.operator = "between";
    filter.value = now.clone().subtract(filter.value, filter.timeUnit).startOf(filter.timeUnit).toDate();
    filter.value2 = now.clone().endOf("day").toDate();
    delete filter.timeUnit;
  },
  "$inNext": function (filter) {
    var now = moment().locale(ui.getBrowserLocale());
    filter.operator = "between";
    filter.value2 = now.clone().add(filter.value, filter.timeUnit).endOf(filter.timeUnit).toDate();
    filter.value = now.clone().startOf("day").toDate();
    delete filter.timeUnit;
  },
  "$inCurrent": function (filter) {
    var now = moment().locale(ui.getBrowserLocale());
    filter.operator = "between";
    filter.value = now.clone().startOf(filter.timeUnit).toDate();
    filter.value2 = now.clone().endOf(filter.timeUnit).toDate();
    delete filter.timeUnit;
  },
  "$isCurrentUser": function (filter) {
    filter.operator = "=";
    filter.value = axelor.config["user.id"];
  },
  "$isCurrentGroup": function (filter) {
    filter.operator = "=";
    filter.value = axelor.config["user.group"];
  }
};

function sharedProperty(scope, handler, property, initialValue) {
  var ds = handler._dataSource;
  if (ds) {
    var adv = ds._advSearch || (ds._advSearch = {});
    adv[property] = adv[property] || initialValue;
    Object.defineProperty(scope, property, {
      get: function () { return adv[property]; },
      set: function (value) { adv[property] = value; }
    });
  }
}

ui.directive('uiFilterItem', function() {

  return {
    replace: true,
    require: '^uiFilterForm',
    scope: {
      fields: "=",
      filter: "=",
      model: "="
    },
    link: function(scope, element, attrs, form) {

      function getOperators() {

        if (element.is(':hidden')) {
          return;
        }

        var filter = scope.filter || {};
        if (filter.type === undefined || !filter.field) {
          return [];
        }

        var field = scope.fields[filter.field] || {};
        var operators = filter.selectionList
          ? OPERATORS_BY_TYPE["enum"] || []
          : OPERATORS_BY_TYPE[filter.type] || [];

        if (field.target && !field.targetName) {
          operators = ["isNull", "notNull"];
        }

        var extraOperators = EXTRA_OPERATORS_BY_TARGET[field.target];
        if (!_.isEmpty(extraOperators)) {
          operators = operators.concat(extraOperators);
        }

        return _.map(operators, function(name) {
          return {
            name: name,
            title: OPERATORS[name]
          };
        });
      }

      scope.remove = function(filter) {
        form.removeFilter(filter);
      };

      scope.canShowSelect = function () {
        var canShow = (CAN_SHOW[scope.filter.operator] || {})['select'];
        if (canShow !== undefined) {
          return canShow;
        }
        return scope.filter && scope.filter.selectionList &&
             scope.filter.operator && !(
             scope.filter.operator == 'isNull' ||
             scope.filter.operator == 'notNull');
      };

      scope.canShowTags = function() {
        var canShow = (CAN_SHOW[scope.filter.operator] || {})['tags'];
        if (canShow !== undefined) {
          return canShow;
        }
        return scope.filter &&
           ['many-to-one', 'one-to-one', 'many-to-many'].indexOf(scope.filter.type) > -1
           && ( scope.filter.operator == 'in' ||
            scope.filter.operator == 'notIn');
      };

      scope.canShowInput = function() {
        var canShow = (CAN_SHOW[scope.filter.operator] || {})['input'];
        if (canShow !== undefined) {
          return canShow;
        }
        return scope.filter && !scope.canShowSelect() && !scope.canShowTags() &&
             scope.filter.operator && !(
             scope.filter.type == 'boolean' ||
             scope.filter.operator == 'isNull' ||
             scope.filter.operator == 'notNull');
      };

      scope.canShowRange = function() {
        var canShow = (CAN_SHOW[scope.filter.operator] || {})['range'];
        if (canShow !== undefined) {
          return canShow;
        }
        return scope.filter && (
             scope.filter.operator === 'between' ||
             scope.filter.operator === 'notBetween');
      };

      scope.canShowTimeUnit = function() {
        var canShow = (CAN_SHOW[scope.filter.operator] || {})['timeUnit'];
        if (canShow !== undefined) {
          return canShow;
        }
        return false;
      };

      scope.getSelection = function () {
        if (!scope.canShowSelect()) return [];
        var field = (scope.fields||{})[scope.filter.field] || {};
        return field.selectionList || [];
      };

      scope.getTimeUnitSelection = function () {
        return TIME_UNIT_SELECTIONS[scope.filter.operator];
      };

      scope.onFieldChange = function() {
        var filter = scope.filter,
          field = scope.fields[filter.field] || {};

        filter.type = field.type || 'string';
        filter.selectionList = field.selectionList;
        filter.value = undefined;
        filter.value2 = undefined;

        if (field.type === 'many-to-one' || field.type === 'one-to-one') {
          filter.targetName = field.targetName;
        } else {
          filter.targetName = null;
        }
      };

      scope.onOperatorChange = function() {
        if (scope.canShowTimeUnit()) {
          if (!Number.isInteger || !Number.isInteger(scope.filter.value)) {
            scope.filter.value = undefined;
          }
          if (!scope.filter.timeUnit) {
            scope.filter.timeUnit = scope.getTimeUnitSelection()[0].value;
          }
        }
        setTimeout(function() {
          scope.$parent.$parent.$parent.doAdjust();
        });
      };

      scope.$watch('filter.field', function searchFilterFieldWatch(value, old) {
        scope.operators = getOperators();
      });

      scope.$on('on:show-menu', function () {
        scope.operators = getOperators();
      });

      scope.getOptions = function () {
        var all = [];
        var data = scope.$parent.contextData || {};
        var field = data.field || {};
        _.each(scope.options, function (item) {
          var name = field.name;
          if (name && item.name === name && data.value) {
            return;
          }
          if (item.contextField && !(item.contextField === name && item.contextFieldValue === data.value)) {
            return;
          }
          all.push(item);
        });
        return all;
      };

      var unwatch = scope.$watch('fields', function searchFiledsWatch(fields, old) {
        if (_.isEmpty(fields)) return;
        unwatch();
        var options = _.values(fields);
        scope.options = _.sortBy(options, function (x) { return (x.title||'').toLowerCase(); });
      }, true);
    },
    template:
      "<div class='flex-layout'>" +
        "<div class='flex-row'>" +
          "<div class='flex-item filter-remove'>" +
            "<a href='' ng-click='remove(filter)'><i class='fa fa-times'></i></a>" +
          "</div>" +
          "<div class='flex-item filter-inputs'>" +
            "<span>" +
              "<select ng-model='filter.field' ng-options='v.name as v.title for v in getOptions()' ng-change='onFieldChange()' class='input-medium'></select> " +
            "</span>" +
            "<span>" +
              "<select ng-model='filter.operator' ng-options='o.name as o.title for o in operators' ng-change='onOperatorChange()' class='input-medium'></select> "+
            "</span>" +
            "<span ng-show='canShowSelect()'>" +
              "<select ng-model='filter.value' class='input=medium' ng-options='o.value as o.title for o in getSelection()'></select>" +
            "</span>" +
            "<span ng-if='canShowTags()'>" +
              "<div ui-filter-tags x-filter='filter' x-model='model' x-fields='fields'></div>" +
            "</span>" +
            "<span ng-show='canShowInput()'>" +
              "<input type='text' ui-filter-input ng-model='filter.value' class='input-medium'> " +
            "</span>" +
            "<span ng-show='canShowRange()'>" +
              "<input type='text' ui-filter-input ng-model='filter.value2' class='input-medium'> " +
            "</span>" +
            "<span ng-show='canShowTimeUnit()'>" +
              "<select ng-model='filter.timeUnit' class='input=medium' ng-options='o.value as o.title for o in getTimeUnitSelection()'></select>" +
            "</span>" +
          "</div>" +
        "</div>" +
      "</div>"
  };
});

ui.directive('uiFilterTags', function() {
  return {
    scope: {
      filter: '=',
      fields: '=',
      model: '='
    },
    controller: ['$scope', '$element', 'DataSource', 'ViewService',
      function ($scope, $element, DataSource, ViewService) {

      var filter = $scope.filter || {};
      var fields = $scope.fields || {};

      var field = _.extend({}, fields[filter.field], {
        required: false,
        readonly: false,
        widget: 'tag-select',
        showTitle: false,
        canNew: false,
        canEdit: false,
        colSpan: 12
      });

      var schema = {
        cols: 1,
        type: 'form',
        items: [{
          type: 'panel',
          items: [field]
        }]
      };

      $scope._viewParams = {
        model: $scope.model,
        views: [schema],
        fields: fields
      };

      ui.ViewCtrl($scope, DataSource, ViewService);
      ui.FormViewCtrl.call(this, $scope, $element);

      $scope.schema = schema;
      $scope.schema.loaded = true;

      $scope.$watch('record.' + filter.field, function searchFilterFieldWatch(value) {
        filter.value = _.pluck(value, 'id');
      });

      function fetchValues(value) {
        $scope._dataSource._new(field.target).search({
          fields: _.compact([field.targetName]),
          domain: 'self.id in (:ids)',
          context: { ids: value }
        }).success(function (records) {
          var record = {};
          var names = filter.field.split('.');
          var rec = record;
          while (names.length > 1) {
            rec = rec[names.shift()] = {};
          }
          rec[names[0]] = records;
          $scope.edit(record);
        });
      }

      $scope.defaultValues = {};
      $scope.editRecord = function (record) {
        if (record && record !== $scope.defaultValues) {
          $scope.record = record;
        }
      };

      $scope.setEditable();
      $scope.show();

      if (_.isArray(filter.value) && filter.value.length) {
        fetchValues(filter.value);
      }
    }],
    template:
      "<div ui-view-form x-handler='true'></div>"
  };
});

ui.directive('uiFilterInput', function() {

  return {
    require: '^ngModel',

    link: function(scope, element, attrs, model) {

      var picker = null;
      var pattern = /^(\d{2}\/\d{2}\/\d{4})$/;
      var isopattern = /^(\d{4}-\d{2}-\d{2}T.*)$/;

      var options = {
        dateFormat: ui.dateFormat.toLowerCase().replace('yyyy', 'yy'),
        showButtonsPanel: false,
        showTime: false,
        showOn: null,
        onSelect: function(dateText, inst) {
          var value = picker.datepicker('getDate');
          var isValue2 = _.str.endsWith(attrs.ngModel, 'value2');

          value = isValue2 ? moment(value).endOf('day').toDate() :
                           moment(value).startOf('day').toDate();

          model.$setViewValue(value.toISOString());
        },
        onClose: function (dateText, inst) {
          picker.datepicker('destroy');
          picker = null;
        }
      };

      model.$formatters.push(function(value) {
        if (_.isDate(value)) {
          value = moment(value).format(ui.dateFormat);
        }
        return value;
      });

      model.$parsers.push(function(value) {
        if (scope.canShowTimeUnit()) {
          return parseInt(value);
        }
        if (/^date/.test(scope.filter.type)) {
          if (isopattern.test(value)) {
            return value;
          } else if (pattern.test(value)) {
            var isValue2 = _.str.endsWith(attrs.ngModel, 'value2');
            return isValue2 ? moment(value, ui.dateFormat).endOf('day').toDate() :
                      moment(value, ui.dateFormat).startOf('day').toDate();
          }
          return null;
        }
        return value;
      });

      model.$parsers.push(function(value) {
        if (scope.canShowTimeUnit()) {
          return value;
        }
        var type = scope.filter.type;
        if (!(type == 'date' || type == 'datetime') || isDate(value)) {
          return value;
        }
        return toMoment(value).toDate();
      });

      function isDate(value) {
        if (value === null || value === undefined) return true;
        if (_.isDate(value)) return true;
        if (/\d+-\d+-\d+T/.test(value)) return true;
      }

      function toMoment(value) {
        var format = null;
        if (/\d+\/\d+\/\d+/.test(value)) format = ui.dateFormat;
        if (/\d+\/\d+\/\d+\s+\d+:\d+/.test(value)) format = ui.dateTimeFormat;
        if (format === null) {
          return moment();
        }
        return moment(value, format);
      }

      element.focus(function(e) {
        var type = scope.filter.type;
        if (!(type == 'date' || type == 'datetime') || scope.canShowTimeUnit()) {
          return;
        }
        picker = picker || element.datepicker(options);
        picker.datepicker('show');
      });

      element.on('$destroy', function() {
        if (picker) {
          picker.datepicker('destroy');
          picker = null;
        }
      });
    }
  };
});

ui.directive('uiFilterContext', function () {

  return {
    scope: {
      fields: '=',
      context: '='
    },
    controller: ['$scope', function ($scope) {
      $scope.field = {
        name: 'contextValue',
        evalTarget: 'context.field.target',
        evalTargetName: 'context.field.targetName',
        evalValue: 'context.value',
        evalTitle: 'context.title'
      };

      $scope.getViewDef = function () {
        return $scope.field;
      };

      $scope.remove = function () {
        var context = {};
        var fields = $scope.contextFields || [];
        if (fields.length === 1) {
          context.field = fields[0];
        }
        $scope.context = context;
      };

      $scope.$watch('context.field.name', function searchContextFieldWatch(name) {
        if (!name) {
          $scope.remove();
        }
      });

      $scope.onFields = function (fields) {
        var contextFields = {};
        for (var item in fields) {
          var field = fields[item];
          var name = field.contextField;
          if (name && fields[name] && !contextFields[name]) {
            contextFields[name] = fields[name];
          }
        }
        $scope.contextFields = _.sortBy(_.values(contextFields), function (x) { return (x.title||'').toLowerCase(); });
        $scope.remove();
      };
    }],
    link: function (scope, element, attrs) {
      var unwatch = scope.$watch('fields', function searchContextFieldsWatch(fields) {
        if (_.isEmpty(fields)) return;
        unwatch();
        scope.onFields(fields);
      }, true);
    },
    template:
      "<div class='flex-layout filter-context' ng-show='contextFields.length'>" +
        "<div class='flex-row'>" +
          "<div class='flex-item filter-remove'>" +
            "<a href='' ng-click='remove()'><i class='fa fa-times'></i></a>" +
          "</div>" +
          "<div class='flex-item filter-inputs'>" +
            "<span>" +
              "<select ng-model='context.field' ng-options='v as v.title for v in contextFields'></select>" +
            "</span>" +
            "<span>" +
              "<span ui-eval-ref-select ng-model='context.value' x-field='contextValue'></span>" +
            "</span>" +
          "</div>" +
        "</div>" +
      "</div>"
  };
});

FilterFormCtrl.$inject = ['$scope', '$element', 'ViewService'];
function FilterFormCtrl($scope, $element, ViewService) {

  this.doInit = function(model, viewItems) {
    var context = $scope.$parent.$parent._context || {};
    return ViewService
    .getFields(model, context.jsonModel)
    .success(function(fields, jsonFields) {

      var items = {};
      var nameField = null;

      _.each(fields, function(field, name) {
        if (field.name === 'id' || field.name === 'version' ||
          field.name === 'archived' || field.name === 'selected') return;
        if (field.type === 'binary' || field.large || field.encrypted) return;
        if (field.nameColumn) {
          nameField = name;
        }
        items[name] = field;
      });

      // include json fields
      _.each(jsonFields, function (fields, prefix) {
        _.each(fields, function (field, name) {
          if (['button', 'panel', 'separator', 'many-to-many'].indexOf(field.type) > -1) return;
          var key = prefix + '.' + name;
          if (field.type !== 'many-to-one') {
            key += '::' + (field.jsonType || 'text');
          }
          items[key] = _.extend({}, field, {
            name: key,
            title: (field.title || field.autoTitle) + " (" + items[prefix].title + ")"
          });
        });
        // don't search parent
        delete items[prefix];
      });

      if (!nameField) {
        nameField = (fields.name || {}).name;
      }

      _.each(viewItems, function (item) {
        if (item.hidden) {
          delete items[item.name];
        } else {
          items[item.name] = item;
        }
      });

      var contextFieldNames = [];
      for (var item in items) {
        var field = items[item];
        var name = field.contextField;
        if (name && items[name] && contextFieldNames.indexOf(name) === -1) {
          contextFieldNames.push(name);
        }
      }
      $scope.fields = items;
      $scope.contextFieldNames = contextFieldNames;

      $scope.$parent.fields = $scope.fields;
      $scope.$parent.contextFieldNames = $scope.contextFieldNames;
      $scope.$parent.nameField = nameField || ($scope.fields.name ? 'name' : null);
    });
  };

  $scope.fields = {};

  sharedProperty($scope, $scope.$parent.handler, 'filters', [{ $new: true }]);
  sharedProperty($scope, $scope.$parent.handler, 'operator', 'and');
  sharedProperty($scope, $scope.$parent.handler, 'showArchived', false);

  var handler = $scope.$parent.handler;
  if (handler && handler._dataSource) {
    $scope.showArchived = handler._dataSource._showArchived;
  }

  $scope.addFilter = function(filter) {
    var last = _.last($scope.filters);
    if (last && !(last.field && last.operator)) return;
    $scope.filters.push(filter || { $new: true });
  };

  this.removeFilter = function(filter) {
    var index = $scope.filters.indexOf(filter);
    if (index > -1) {
      $scope.filters.splice(index, 1);
    }
    if ($scope.filters.length === 0) {
      $scope.addFilter();
    }
  };

  $scope.$on('on:select-custom', function(e, custom) {

    $scope.filters.length = 0;
    $scope.contextData = {};

    if (custom.$selected) {
      select(custom);
    } else {
      $scope.addFilter();
    }

    return $scope.applyFilter();
  });

  $scope.$on('on:select-domain', function(e, filter) {
    $scope.filters.length = 0;
    $scope.addFilter();
    return $scope.applyFilter();
  });

  $scope.$on('on:before-save', function(e, data) {
    var criteria = $scope.prepareFilter();
    if (data) {
      data.criteria = criteria;
    }
  });

  $scope.$on('on:apply-filter', function (e, hide, applyingDefaults) {
    $scope.applyFilter(hide, applyingDefaults);
  });

  $scope.$on('on:clear-filter', function (e, options) {
    $scope.clearFilter(options);
  });

  function select(custom) {

    var criteria = custom.criteria;
    var filters = criteria.criteria;
    var contextFieldNames = $scope.contextFieldNames || [];

    if (filters && filters.length && filters.length < 3) {
      var first = _.first(filters);
      var last = _.last(filters);
      var name = first.fieldName.replace('.id', '');
      if (contextFieldNames.indexOf(name) > -1) {
        $scope.contextData = {
            field: $scope.fields[name],
            value: first.value,
            title: first.title,
            saved: true
        };
        filters = (last||{}).criteria || [{}];
      }
    }

    $scope.operator = criteria.operator || 'and';

    _.each(filters, function(item) {

      var fieldName = item.fieldName || '';
      if (fieldName && $scope.fields[fieldName] === undefined && fieldName.indexOf('.') > -1 && fieldName.indexOf('::') === -1) {
        fieldName = fieldName.substring(0, fieldName.lastIndexOf('.'));
      }

      var field = $scope.fields[fieldName] || {};
      var filter = {
        field: fieldName,
        value: item.value,
        value2: item.value2
      };

      filter.type = field.type || 'string';
      filter.operator = item.operator;

      if (field.selectionList) {
        filter.selectionList = field.selectionList;
      }

      var criterionPreparator = CRITERION_PREPARATORS[item.operator];
      if (criterionPreparator) {
        criterionPreparator(filter, item);
      } else {
        if (item.operator === '=' && filter.value === true) {
          filter.operator = 'true';
        }
        if (filter.operator === '=' && filter.value === false) {
          filter.operator = 'false';
        }

        if (field.type === 'date' || field.type === 'datetime') {
          if (filter.value) {
            filter.value = moment(filter.value).toDate();
          }
          if (filter.value2) {
            filter.value2 = moment(filter.value2).toDate();
          }
        }

        if (filter.type == 'many-to-one' || field.type === 'one-to-one') {
          filter.targetName = field.targetName;
        }
      }
      $scope.addFilter(filter);
    });
  }

  $scope.clearFilter = function(options) {
    $scope.filters.length = 0;
    $scope.showArchived = false;
    $scope.addFilter();
    $scope.contextData = {};

    if ($scope.$parent.onClear) {
      $scope.$parent.onClear();
    }

    var hide = options === true;
    var silent = !hide && options && options.silent;

    if (!silent) {
      $scope.applyFilter();
    }

    if ($scope.$parent && hide) {
      $scope.$parent.$broadcast('on:hide-menu');
    }
  };

  $scope.prepareFilter = function() {

    var criteria = {
      archived: $scope.showArchived,
      operator: $scope.operator,
      criteria: []
    };

    _.each($scope.filters, function(filter) {

      if (!filter.field || !filter.operator) {
        return;
      }

      var criterion = {
        fieldName: filter.field,
        operator: filter.operator,
        value: filter.value
      };

      if (filter.operator == 'like' || filter.operator == 'notLike') {
        criterion.value = criterion.value || '';
      }

      var criterionPreparator = CRITERION_PREPARATORS[filter.operator];
      if (criterionPreparator) {
        criterionPreparator(criterion, filter);
      } else if (filter.operator == 'in' ||
        filter.operator == 'notIn') {
        if (_.isEmpty(filter.value)) {
          return;
        }
        criterion.fieldName += '.id';
      } else if (filter.targetName && criterion.fieldName.indexOf(':') == -1 && (
          filter.operator !== 'isNull' ||
          filter.operator !== 'notNull')) {
        criterion.fieldName += '.' + filter.targetName;
      } else if (/-many/.test(filter.type) && (
          filter.operator !== 'isNull' ||
          filter.operator !== 'notNull')) {
        criterion.fieldName += '.id';
      }

      if (criterion.operator == "true") {
        criterion.operator = "=";
        criterion.value = true;
      }
      if (criterion.operator == "false") {
        criterion = {
          operator: "or",
          criteria: [
              {
                fieldName: filter.field,
                operator: "=",
                value: false
              },
              {
                fieldName: filter.field,
                operator: "isNull"
              }
          ]
        };
      }

      if (filter.type === 'string' || filter.type === 'text') {
        if (criterion.operator == "isNull") {
          criterion = {
            operator: "or",
            criteria: [
                {
                  fieldName: filter.field,
                  operator: "isNull"
                },
                {
                  fieldName: filter.field,
                  operator: "=",
                  value: ''
                }
            ]
          };
        }
        if (criterion.operator == "notNull") {
          criterion = {
            operator: "and",
            criteria: [
                {
                  fieldName: filter.field,
                  operator: "notNull"
                },
                {
                  fieldName: filter.field,
                  operator: "!=",
                  value: ''
                }
            ]
          };
        }
      }

      if (criterion.operator == "between" || criterion.operator == "notBetween") {
        criterion.value2 = filter.value2;
      }

      if (filter.$new) {
        criterion.$new = true;
      }

      criteria.criteria.push(criterion);
    });

    var contextData = $scope.contextData || {};

    if (contextData.value && contextData.field && contextData.field.name) {
      var previous = criteria.criteria;
      var operator = criteria.operator;

      criteria.operator = "and";
      criteria.criteria = [{
        fieldName: contextData.field.name + ".id",
        operator: "=",
        value: contextData.value,
        title: contextData.title,
        $new: !contextData.saved
      }];

      if (previous && previous.length) {
        criteria.criteria.push({
          operator: operator,
          criteria: previous
        });
      }
    }

    return criteria;
  };

  var appliedFilters = false;
  var appliedContext = false;

  $scope.$watch('filters', function (fitlers, old) {
    appliedFilters = fitlers === old;
  }, true);

  $scope.$watch('contextData', function (data, old) {
    appliedContext = data === old;
  }, true);

  $scope.applyFilter = function(hide, applyingDefaults) {
    var criteria = $scope.prepareFilter();
    var promise;
    if ($scope.$parent.onFilter) {
      promise = $scope.$parent.onFilter(criteria, applyingDefaults);
    }
    if ($scope.$parent && hide) {
      $scope.$parent.$broadcast('on:hide-menu');
    }
    handler.$broadcast('on:advance-filter', criteria);
    handler.$broadcast('on:context-field-change', $scope.contextData);
    appliedFilters = true;
    appliedContext = true;
    return promise;
  };

  $scope.canExport = function(full) {
    var allowFull = axelor.config["view.adv-search.export.full"] !== false;
    var handler = $scope.$parent.handler;
    if (handler && handler.hasPermission) {
      return full ? allowFull && handler.hasPermission('export') : handler.hasPermission('export');
    }
    return true;
  };

  $scope.onExport = function(full) {
    var handler = $scope.$parent.handler;
    if (handler && handler.onExport) {
      var promise = appliedFilters && appliedContext ? null : $scope.applyFilter(true);
      if (promise && promise.then) {
        promise.then(function () {
          handler.onExport(full);
        });
      } else {
        handler.onExport(full);
      }
    }
  };
}

ui.directive('uiFilterForm', function() {

  return {
    replace: true,

    scope: {
      model: '=',
      onSearch: '&'
    },

    controller: FilterFormCtrl,

    link: function(scope, element, attrs, ctrl) {
      var unwatch = scope.$watch("$parent.viewItems", function searchViewItemsWatch(items) {
        if (items === undefined) return;
        unwatch();
        ctrl.doInit(scope.model, items);
      });
    },
    template:
    "<div class='filter-form'>" +
      "<div ui-filter-context fields='fields' context='contextData'></div>" +
      "<form class='filter-operator form-inline'>" +
        "<label class='radio inline'>" +
          "<input type='radio' name='operator' ng-model='operator' value='and' x-translate><span x-translate>and</span>" +
        "</label>" +
        "<label class='radio inline'>" +
          "<input type='radio' name='operator' ng-model='operator' value='or' x-translate><span x-translate>or</span>" +
        "</label>" +
        "<label class='checkbox inline show-archived'>" +
          "<input type='checkbox' ng-model='showArchived'><span x-translate>Show archived</span>" +
        "</label>" +
      "</form>" +
      "<div ng-repeat='filter in filters' ui-filter-item x-model='model' x-fields='fields' x-filter='filter'></div>" +
      "<div class='links'>"+
        "<a href='' ng-click='addFilter()' x-translate>Add filter</a>"+
        "<span class='divider'>|</span>"+
        "<a href='' ng-click='clearFilter(true)' x-translate>Clear</a></li>"+
        "<span class='divider'>|</span>"+
        "<a href='' ng-if='canExport()' ng-click='onExport()' x-translate>Export</a></li>"+
        "<span class='divider' ng-if='canExport()'>|</span>"+
        "<a href='' ng-if='canExport(true)' ng-click='onExport(true)' x-translate>Export full</a></li>"+
        "<span class='divider' ng-if='canExport(true)'>|</span>"+
        "<a href='' ng-click='applyFilter(true)' x-translate>Apply</a></li>"+
      "<div>"+
    "</div>"
  };
});

ui.directive('uiFilterBox', function() {

  return {
    scope: {
      handler: '='
    },
    controller: ['$scope', 'ViewService', 'DataSource', function($scope, ViewService, DataSource) {

      var handler = $scope.handler,
        params = (handler._viewParams || {}).params;

      var filterView = params ? params['search-filters'] : null;
      var filterDS = DataSource.create('com.axelor.meta.db.MetaFilter');

      this.$scope = $scope;

      $scope.model = handler._model;
      $scope.view = {};

      sharedProperty($scope, $scope.handler, 'viewFilters', []);
      sharedProperty($scope, $scope.handler, 'custFilters', []);
      sharedProperty($scope, $scope.handler, 'tagItems', []);

      $scope.canShare = axelor.config["view.adv-search.share"] !== false;

      if (filterView) {
        var defaultFilters = params && params['default-search-filters'];
        if (defaultFilters) {
          $scope.handler.beforeOnShowEventName = 'on:default-search-filters-applied';
        }

        ViewService.getMetaDef($scope.model, {name: filterView, type: 'search-filters'})
        .success(function(fields, view) {
          var viewItems = _.map(view.items, function (item) {
            var field = fields[item.name] || {};
            return _.extend({}, field, item, { type: field.type });
          });
          $scope.view = view;
          $scope.viewItems = viewItems;
          $scope.viewFilters = angular.copy(view.filters);

          if (defaultFilters) {
            defaultFilters = defaultFilters.split(/\s*,\s*/);
            _.each($scope.viewFilters, function (filter) {
              if (_.contains(defaultFilters, filter.name)) {
                filter.$selected = true;
                $scope.selectFilter(filter, false, false);
              }
            });
            $scope.$broadcast('on:apply-filter', false, true);
          }
        });
      } else {
        $scope.viewItems = [];
        filterView = 'act:' + (handler._viewParams || {}).action;
      }

      if (filterView) {
        filterDS.rpc('com.axelor.meta.web.MetaFilterController:findFilters', {
          model: 'com.axelor.meta.db.MetaFilter',
          context: {
            filterView: filterView
          }
        }).success(function(res) {
          _.each(res.data, function(item) {
            acceptCustom(item);
          });
        });
      }

      var current = {
        criteria: {},
        domains: [],
        customs: []
      };

      function acceptCustom(filter) {

        var custom = {
          title: filter.title,
          name: filter.name,
          shared: filter.shared,
          criteria: angular.fromJson(filter.filterCustom)
        };
        custom.selected = filter.filters ? filter.filters.split(/\s*,\s*/) : [];
        custom.selected = _.map(custom.selected, function(x) {
          return parseInt(x);
        });

        var found = _.findWhere($scope.custFilters, {name: custom.name});
        if (found) {
          _.extend(found, custom);
        } else {
          $scope.custFilters.push(custom);
        }

        return found ? found : custom;
      }

      $scope.selectFilter = function(filter, isCustom, live) {

        var selected = live ? !filter.$selected : filter.$selected;
        var selection = isCustom ? current.customs : current.domains;
        var applyAll = (handler.schema||{}).customSearch === false;

        if (live) {
          $scope.onClear();
        }

        filter.$selected = selected;

        var index = selection.indexOf(filter);
        if (selected) {
          selection.push(filter);
        }
        if (!selected && index > -1) {
          selection.splice(index, 1);
        }

        if (isCustom && (live || applyAll)) {
          $scope.hasCustSelected = filter.$selected && !applyAll;
          $scope.custName = filter.$selected ? filter.name : null;
          $scope.oldCustTitle = filter.$selected ? filter.title : '';
          $scope.custTitle = filter.$selected ? filter.title : '';
          $scope.custShared = filter.$selected ? filter.shared : false;
          return $scope.$broadcast('on:select-custom', filter, selection);
        }

        if (live || applyAll) {
          $scope.$broadcast('on:select-domain', filter);
        }
      };

      $scope.isSelected = function(filter) {
        return filter.$selected;
      };

      $scope.onRefresh = function() {
        if (this.custTerm) {
          return this.onFreeSearch();
        }
        $scope.$broadcast('on:apply-filter', true);
      };

      $scope.onReset = function() {
        $scope.$broadcast('on:clear-filter-silent');
      };

      $scope.hasFilters = function(which) {
        if (which === 1) {
          return this.viewFilters && this.viewFilters.length;
        }
        if (which === 2) {
          return this.custFilters && this.custFilters.length;
        }
        return (this.viewFilters && this.viewFilters.length) ||
             (this.custFilters && this.custFilters.length);
      };

      $scope.canSaveNew = function() {
        if ($scope.hasCustSelected && $scope.oldCustTitle && $scope.custTitle) {
          return !angular.equals(_.underscored($scope.oldCustTitle), _.underscored($scope.custTitle));
        }
        return false;
      };

      $scope.onSave = function(saveAs) {

        var data = { criteria: null };
        $scope.$broadcast('on:before-save', data);

        var title = _.trim($scope.custTitle),
          name = $scope.custName || _.underscored(title);

        if (saveAs) {
          name = _.underscored(title);
        }

        var selected = [];

        _.each($scope.viewFilters, function(item, i) {
          if (item.$selected) selected.push(i);
        });

        var custom = data.criteria || {};

        custom = _.extend({
          operator: custom.operator,
          criteria: custom.criteria
        });

        var value = {
          name: name,
          title: title,
          shared: $scope.custShared,
          filters: selected.join(', '),
          filterView: filterView,
          filterCustom: angular.toJson(custom)
        };

        filterDS.rpc('com.axelor.meta.web.MetaFilterController:saveFilter', {
          model: 'com.axelor.meta.db.MetaFilter',
          context: value
        }).success(function(res) {
          var custom = acceptCustom(res.data);
          custom.$selected  = false;
          $scope.selectFilter(custom, true, true);
        });
      };

      $scope.onDelete = function() {

        var name = $scope.custName;
        if (!$scope.hasCustSelected || !name) {
          return;
        }

        function doDelete() {
          filterDS.rpc('com.axelor.meta.web.MetaFilterController:removeFilter', {
            model: 'com.axelor.meta.db.MetaFilter',
            context: {
              name: name,
              filterView: filterView
            }
          }).success(function(res) {
            var found = _.findWhere($scope.custFilters, {name: name});
            if (found) {
              $scope.custFilters.splice($scope.custFilters.indexOf(found), 1);
              $scope.hasCustSelected = false;
              $scope.custName = null;
              $scope.custTitle = null;
              $scope.custShared = false;
            }
            $scope.onFilter();
          });
        }

        axelor.dialogs.confirm(_t("Would you like to remove the filter?"), function(confirmed){
          if (confirmed) {
            doDelete();
          }
        });
      };

      $scope.onClear = function() {

        _.each($scope.viewFilters, function(d) { d.$selected = false; });
        _.each($scope.custFilters, function(d) { d.$selected = false; });

        current.domains.length = 0;
        current.customs.length = 0;

        $scope.hasCustSelected = false;
        $scope.custName = null;
        $scope.oldCustTitle = null;
        $scope.custTitle = null;
        $scope.custShared = false;
        $scope.custTerm = null;
        $scope.tagItems = [];

        if ($scope.handler && $scope.handler.clearFilters) {
          $scope.handler.clearFilters();
        }
      };

      $scope.onFilter = function(criteria, applyingDefaults) {

        if (criteria) {
          current.criteria = criteria;
        } else {
          criteria = current.criteria;
        }

        var search = _.extend({}, criteria);
        if (!search.criteria) {
          search.operator = 'and';
          search.criteria = [];
        } else {
          search.criteria = _.clone(search.criteria);
        }

        if (arguments.length > 1 && _.isString(arguments[1])) {
          search._searchText = arguments[1];
        }

        var domains = [],
          customs = [];

        _.each(current.domains, function(domain) {
          domains.push(domain);
        });

        _.each(current.customs, function(custom) {
          if($scope.hasCustSelected) { return; }
          if (custom.criteria && custom.criteria.criteria) {
            customs.push({
              operator: custom.criteria.operator || 'and',
              criteria: custom.criteria.criteria
            });
          }
          _.each(custom.selected, function(i) {
            var domain = $scope.viewFilters[i];
            if (domains.indexOf(domain) == -1) {
              domains.push(domain);
            }
          });
        });

        if (customs.length > 0) {
          search.criteria.push({
            operator: criteria.operator || 'and',
            criteria: customs
          });
        }

        search._domains = domains;
        search.criteria = process(search.criteria);

        // process criteria for datetime fields, always use between operator
        function process(filter) {
          if (_.isArray(filter)) return _.map(filter, process);
          if (_.isArray(filter.criteria)) {
            filter.criteria = process(filter.criteria);
            return filter;
          }

          var transformer = FILTER_TRANSFORMERS[filter.operator];
          if (transformer) {
            filter.transformer = transformer;
          }

          var name = filter.fieldName;
          var type = (($scope.fields||$scope.$parent.fields||{})[filter.fieldName]||{}).type;

          var v1 = filter.value;
          var v2 = filter.value2;

          // if json date/datetime field
          if (name.indexOf('::') > -1 && (type === 'date' || type === 'datetime')) {
            filter = _.extend({}, filter);
            switch (filter.operator) {
            case '>':
              filter.value = moment(v1).endOf('day').toDate();
              filter.value2 = undefined;
              break;
            case '<':
              filter.value = moment(v1).startOf('day').toDate();
              filter.value2 = undefined;
              break;
            case '=':
              filter.operator = 'between';
              filter.value = moment(v1).startOf('day').toDate();
              filter.value2 = moment(v1).endOf('day').toDate();
              break;
            case '!=':
              filter.operator = 'notBetween';
              filter.value = moment(v1).startOf('day').toDate();
              filter.value2 = moment(v1).endOf('day').toDate();
              break;
            case 'between':
            case 'notBetween':
              filter.value = moment(v1).startOf('day').toDate();
              filter.value2 = moment(v2).endOf('day').toDate();
              break;
            }
            return filter;
          }

          if (filter.operator !== '=') return filter;
          if (type != 'datetime') return filter;

          if (!(/\d+-\d+\d+T/.test(filter.value) || _.isDate(filter.value))) {
            return filter;
          }

          v1 = moment(v1).startOf('day').toDate();
          v2 = moment(v1).endOf('day').toDate();
          return _.extend({}, filter, {
            operator: 'between',
            value: v1,
            value2: v2
          });
        }

        function countCustom(criteria) {
          var n = _.filter(criteria, function (item) { return item.$new; }).length;
          if (criteria.length === 2 && criteria[1].criteria) {
            n += countCustom(criteria[1].criteria);
          }
          return n;
        }

        var tag = {};
        var all = _.chain([$scope.viewFilters, $scope.custFilters])
           .flatten()
           .filter(function (item) {
            return item && item.$selected;
           })
           .pluck('title')
           .value();

        var nCustom = countCustom((criteria||{}).criteria);
        if (nCustom > 0) {
          all.push(_t('Custom ({0})', nCustom));
        }

        if (all.length === 1) {
          tag.title = all[0];
        }
        if (all.length > 1) {
          tag.title = _t('Filters ({0})', all.length);
          tag.help = all.join(', ');
        }

        if (all.length === 0) {
          $scope.tagItems = [];
        } else {
          $scope.tagItems = [tag];
        }

        if (applyingDefaults) {
          $scope.$parent._dataSource._filter = search;
          $scope.$emit('on:default-search-filters-applied');
          return Promise.resolve(search);
        }

        return handler._simpleFilters
          ? handler.filter(handler._simpleFilters, search)
          : handler.filter(search);
      };

      $scope.onFreeSearch = function() {

        var filters = [],
          fields = {},
          text = this.custTerm,
          number = +(text);

        var freeSearch = handler.schema && handler.schema.freeSearch;
        var freeCols = freeSearch && freeSearch !== 'all' ? freeSearch.split(/\s*,\s*/) : [];
        var cols = freeCols.length === 0 ? $scope.findCols() : freeCols;

        text = text ? text.trim() : null;
        fields = _.pick(this.$parent.fields, cols);

        if (text) {
          for(var name in fields) {

            var fieldName = null,
              operator = "like",
              value = text;

            var field = fields[name];

            if (field.transient) continue;

            switch (field.type) {
            case 'integer':
            case 'decimal':
              if (_.isNaN(number) || !_.isNumber(number)) continue;
              if (field.type === 'integer' && (number > 2147483647 || number < -2147483648)) continue;
              fieldName = name;
              operator = '=';
              value = number;
              break;
            case 'text':
            case 'string':
              fieldName = name;
              break;
            case 'one-to-one':
            case 'many-to-one':
              if (field.jsonField) {
                fieldName = name;
              } else if (field.targetName) {
                fieldName = name + '.' + field.targetName;
              }
              break;
            case 'boolean':
              if (/^(t|f|y|n|true|false|yes|no)$/.test(text)) {
                fieldName = name;
                operator = '=';
                value = /^(t|y|true|yes)$/.test(text);
              }
              break;
            }

            if (!fieldName) continue;

            filters.push({
              fieldName: fieldName,
              operator: operator,
              value: value
            });
          }
        }

        var criteria = {
          operator: 'or',
          criteria: filters
        };

        this.onFilter(criteria, text);
      };
    }],
    link: function(scope, element, attrs) {

      var menu = element.children('.filter-menu'),
        toggleButton = null;

      scope.onSearch = function(e) {
        if (menu && menu.is(':visible')) {
          hideMenu();
          return;
        }
        toggleButton = $(e.currentTarget);
        // more than top navbar's zIndex 1030 to hide
        menu.zIndex(element.parent().zIndex() + 1031);
        menu.show();
        scope.doAdjust();

        $(document).on('mousedown.search-menu', onMouseDown);

        scope.$applyAsync(function () {
          scope.visible = true;
          scope.$broadcast('on:show-menu');
        });
      };

      scope.doAdjust = (function() {
        var opts = {
          my: "left top",
          at: "left bottom",
          of: element,
          collision: "fit"
        };
        if (element.hasClass('pull-right')) {
          opts.my = "right top";
          opts.at = "right bottom";
        }
        return function() {
          menu.position(opts);
        };
      }());

      scope.onClearFilter = function () {
        hideMenu();
        scope.visible = true;
        scope.$broadcast('on:clear-filter');
        scope.$timeout(function () {
          scope.visible = false;
        });
      };

      // append menu to body to fix overlaping issue
      scope.$timeout(function() {
        menu.zIndex(element.zIndex() + 1);
        menu.appendTo("body");
      });

      element.on('keydown.search-query', '.search-query', function(e) {
        if (e.keyCode === 13) { // enter
          scope.onFreeSearch();
        }
      });

      scope.$on('on:hide-menu', function () {
        hideMenu();
      });

      scope.$on('on:clear-filter-silent', function () {
        var visible = scope.visible;
        scope.visible = true;
        scope.$broadcast('on:clear-filter', { silent: true });
        scope.$timeout(function () {
          scope.visible = visible;
        });
      });

      function hideMenu() {
        $(document).off('mousedown.search-menu', onMouseDown);
        scope.$timeout(function () {
          scope.visible = false;
        });
        menu.hide();
      }

      function onMouseDown(e) {
        var all = $(menu).add(toggleButton);
        if (all.is(e.target) || all.has(e.target).length > 0) {
          return;
        }
        if ($(e.target).zIndex() > $(menu).zIndex() || $(e.target).parents('.ui-dialog').length) {
          return;
        }
        if(menu) {
          hideMenu();
        }
      }

      scope.hideMenu = hideMenu;

      scope.findCols = function () {
        var grid = element.parents('.grid-view:first').children('[ui-slick-grid]:first').data('grid');
        return grid
          ? _.pluck(grid.getColumns(), 'field').filter(function (n) { return n in scope.$parent.fields; })
          : _.pluck(scope.$parent.fields, 'name');
      };

      scope.handler.$watch('schema.freeSearch', function searchFreeSearchWatch(value, old) {
        if (value === 'none') {
          var input = element.find('input:first')
            .addClass('not-readonly')
            .prop('readonly', true)
            .click(scope.onSearch.bind(scope));
        }
      });

      element.on('$destroy', function() {
        $(document).off('mousedown.search-menu', onMouseDown);
        if (menu) {
          menu.remove();
          menu = null;
        }
      });
    },
    replace: true,
    template:
    "<div class='filter-box'>" +
      "<div class='tag-select picker-input search-query'>" +
        "<ul>" +
        "<li class='tag-item label label-primary' ng-repeat='item in tagItems'>" +
          "<span class='tag-text' title='{{item.help}}'>{{item.title}}</span> " +
          "<i class='fa fa-times fa-small' ng-click='onClearFilter()'></i>" +
        "</li>" +
        "<li class='tag-selector' ng-show='!tagItems.length'>" +
          "<input type='text' autocomplete='off' ng-model='custTerm'>" +
        "</li>" +
        "</ul>" +
        "<span class='picker-icons'>" +
        "<i ng-click='onSearch($event)' class='fa fa-caret-down'></i>"+
        "<i ng-click='onReset()' class='fa fa-eraser'></i>" +
        "<i ng-click='onRefresh()' class='fa fa-search'></i>" +
        "</span>" +
      "</div>" +
      "<div class='filter-menu'>" +
        "<span>" +
          "<strong x-translate>Advanced Search</strong>" +
          "<a href='' class='pull-right' ng-click='hideMenu()'><i class='fa fa-times'></i></a>" +
        "</span>" +
        "<hr>"+
        "<div class='filter-list'>" +
          "<dl ng-show='!hasFilters() && handler.schema.customSearch == false' style='display: hidden;'>" +
            "<dd><span x-translate>No filters available</span></dd>" +
          "</dl>" +
          "<dl ng-show='hasFilters(1)'>" +
            "<dt><i class='fa fa-floppy-o'></i><span x-translate> Filters</span></dt>" +
            "<dd ng-repeat='filter in viewFilters' class='checkbox'>" +
              "<input type='checkbox' " +
                "ng-model='filter.$selected' " +
                "ng-click='selectFilter(filter, false, false)' ng-disabled='hasCustSelected'> " +
              "<a href='' ng-click='selectFilter(filter, false, true)' ng-disabled='hasCustSelected'>{{filter.title}}</a>" +
            "</dd>" +
          "</dl>" +
          "<dl ng-show='hasFilters(2)'>" +
            "<dt><i class='fa fa-filter'></i><span x-translate> My Filters</span></dt>" +
            "<dd ng-repeat='filter in custFilters' class='checkbox'>" +
              "<input type='checkbox' " +
                "ng-model='filter.$selected' " +
                "ng-click='selectFilter(filter, true, false)' ng-disabled='hasCustSelected'> " +
              "<a href='' ng-click='selectFilter(filter, true, true)' ng-disabled='!filter.$selected && hasCustSelected'>{{filter.title}}</a>" +
            "</dd>" +
          "</dl>" +
        "</div>" +
        "<div ng-hide='handler.schema.customSearch == false'>" +
          "<hr ng-show='hasFilters()'>" +
          "<div ui-filter-form x-model='model'></div>" +
          "<hr>" +
          "<div class='form-inline'>" +
            "<div class='control-group'>" +
              "<input type='text' placeholder='{{\"Save filter as\" | t}}' ng-model='custTitle'> " +
              "<label class='checkbox' ng-show='canShare'>" +
                "<input type='checkbox' ng-model='custShared'><span x-translate>Share</span>" +
              "</label>" +
            "</div>" +
            "<button class='btn btn-small' ng-click='onSave()' ng-show='custTitle && !hasCustSelected'><span x-translate>Save</span></button> " +
            "<button class='btn btn-small' ng-click='onSave()' ng-show='custTitle && hasCustSelected'><span x-translate>Update</span></button> " +
            "<button class='btn btn-small' ng-click='onSave(true)' ng-show='canSaveNew()'><span x-translate>Save as</span></button> " +
            "<button class='btn btn-small' ng-click='onDelete()' ng-show='hasCustSelected'><span x-translate>Delete</span></button>" +
          "</div>" +
        "</div>" +
      "</div>" +
    "</div>"
  };
});

})();
