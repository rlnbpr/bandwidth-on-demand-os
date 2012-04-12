var app = app || {};

app.form = function(){

    var init = function() {

        initEventHandlers();

    };

    var initEventHandlers = function() {

        initUserSelection();
        initFormLinks();
        initBandwidthSelector();
        initDatepickers();

    };

    var initUserSelection = function() {

        var form = $('.dropdown-menu');

        form.on('click', 'li', function(event) {

            var item = $(event.target).closest('li')[0];
            var roleId = item.getAttribute('data-roleId');

            $('<input>').attr({
                type: 'hidden',
                name: 'roleId',
                value: roleId
            }).appendTo(form);

            form[0].submit();

        })

    };

    var initFormLinks = function() {

        $('a[data-form]').on('click', function(event) {

            var errorMessage = 'Sorry, action failed.';

            var post = function(url, data) {
                $.post(url, data)
                .success(function() {
                    window.location.reload(true);
                })
                .error(function() {
                    alert(errorMessage);
                });
            };

            var element = $(event.target).closest('a')[0];
            var href = element.href;
            var data = href.replace(/[^\?]*\?/, ''); // Everything after '?'
            var url = href.replace(/\?.*/, ''); // Everything before '?'

            var isToBeConfirmed = element.getAttribute('data-confirm');

            if(isToBeConfirmed) {
                var isConfirmed = confirm(isToBeConfirmed);
                if(isConfirmed) {
                    post(url, data);
                }
            } else {
                post(url, data);
            }

            event.preventDefault();

        })

    };

    var initBandwidthSelector = function() {

        var selectedValues,
            input = $('[data-component="bandwidth-selector"] input'),
            bandwidth = parseInt(input.val());

        var getMaxBandwidth = function() {
            selectedValues = [];
            $('[data-component="bandwidth-selector-source"]').each(function(i, element) {
                selectedValues.push(parseInt($(element).find('option:selected').attr('data-bandwidth-max')));
            });
            return Math.max.apply(null, selectedValues);
        }

        var activateBandwidthButton = function(multiplier) {

            $('[data-component="bandwidth-selector"] button').each(function(i, element) {
                if(parseFloat(element.getAttribute('data-bandwidth-multiplier')) === multiplier) {
                    $(element).addClass('active');
                } else {
                    $(element).removeClass('active');
                }
            });

        }

        $('[data-component="bandwidth-selector"] input').on('change', function(event) {

            bandwidth = parseFloat(event.target.value);

            activateBandwidthButton(bandwidth / getMaxBandwidth());

            event.preventDefault();

        });

        $('[data-component="bandwidth-selector"]').on('click', '.btn', function(event) {

            event.preventDefault();

            if(event.clientX === 0) { return; } // For some weird reason, this 'click' event was also fired on change of the input...?!

            bandwidth = getMaxBandwidth() * parseFloat(event.target.getAttribute('data-bandwidth-multiplier'));

            input.val(bandwidth).trigger('change');

        });

        $('[data-component="bandwidth-selector-source"]').on('change', function() {

            $('[data-component="bandwidth-selector"] button').eq(1).trigger('click');

        });

        activateBandwidthButton(bandwidth / getMaxBandwidth());

    }

    var initDatepickers = function() {
        $(".input-datepicker").datepicker({
            format: 'yyyy-mm-dd',
            autoclose: true
        });
    }

    return {
        init: init
    }

}();

app.register(app.form);
