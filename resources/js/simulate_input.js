/**
 *
 * This file contains a script that is executed in the page context (not in the Password Helper extension context).
 * It is used to simulate user input on the password input fields.
 *
 * It is necessary, because of a bug in Chrome that prevents setting keyCode and which on a KeyboardEvent, which we fire here.
 */

(function() {
    function newKeyboardEvent(type, key) {
        var keyCode = key.charCodeAt(0);
        var event = new KeyboardEvent(type, {
            bubbles: true,
            cancelable: true,
            key: key
        });

        // this is the hack that prevents us from doing this from the extension code
        Object.defineProperty(event, "keyCode", {
            get: function () {
                return keyCode;
            }
        });

        Object.defineProperty(event, "which", {
            get: function () {
                return keyCode;
            }
        });

        return event;
    }

    function newBasicEvent(type) {
        return new Event(type, {
            bubbles: true,
            cancelable: true
        });
    }

    function simulateInput(input, key) {
        input.dispatchEvent(newBasicEvent("focus"));
        input.dispatchEvent(newKeyboardEvent("keydown", key));
        input.dispatchEvent(newKeyboardEvent("keypress", key));
        input.value = key;
        input.dispatchEvent(newKeyboardEvent("keyup", key));
        input.dispatchEvent(newBasicEvent("blur"));
        input.dispatchEvent(newBasicEvent("change"));
        input.dispatchEvent(newBasicEvent("input"));
    }

    document.addEventListener("simulate-input", function (event) {
        // console.log('Sending key "' + event.detail.value + '" to input "' + event.detail.id + '"');
        simulateInput(document.getElementById(event.detail.id), event.detail.value);
    });
})();