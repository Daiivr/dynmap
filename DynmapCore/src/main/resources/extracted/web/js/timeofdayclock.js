componentconstructors['timeofdayclock'] = function(dynmap, configuration) {
	var me = this;
	
	var timeout = null;
	
	var element = $('<div/>')
		.addClass('largeclock')
		.addClass('timeofday')
		.appendTo(dynmap.options.container);
	
	var sun = $('<div/>')
		.height(60)
		.addClass('timeofday')
		.addClass('sun')
		.css('background-position', (-150) + 'px ' + (-150) + 'px')
		.appendTo(element);
	
	var moon = $('<div/>')
		.height(60)
		.addClass('timeofday')
		.addClass('moon')
		.css('background-position', (-150) + 'px ' + (-150) + 'px')
		.appendTo(sun);
	
	if (configuration.showdigitalclock) {
		var clock = $('<div/>')
			.addClass('timeofday')
			.addClass('digitalclock')
			.appendTo(element);
		
		var formatTime = function(time) {
			var formatDigits = function(n, digits) {
				var s = n.toString();
				while (s.length < digits) {
					s = '0' + s;
				}
				return s;
			}
			return formatDigits(time.hours, 2) + ':' + formatDigits(time.minutes, 2);
		};

		// The time is drawn with 5x7 pixel digits and a one-pixel shadow, like in-game text, as SVG
		// so it stays crisp at any size (the page font's 5 reads like a mirrored 2)
		var GLYPHS = {
			'0': [ '.###.', '#...#', '#..##', '#.#.#', '##..#', '#...#', '.###.' ],
			'1': [ '..#..', '.##..', '..#..', '..#..', '..#..', '..#..', '#####' ],
			'2': [ '.###.', '#...#', '....#', '..##.', '.#...', '#....', '#####' ],
			'3': [ '.###.', '#...#', '....#', '..##.', '....#', '#...#', '.###.' ],
			'4': [ '...##', '..#.#', '.#..#', '#...#', '#####', '....#', '....#' ],
			'5': [ '#####', '#....', '####.', '....#', '....#', '#...#', '.###.' ],
			'6': [ '..##.', '.#...', '#....', '####.', '#...#', '#...#', '.###.' ],
			'7': [ '#####', '#...#', '....#', '...#.', '..#..', '..#..', '..#..' ],
			'8': [ '.###.', '#...#', '#...#', '.###.', '#...#', '#...#', '.###.' ],
			'9': [ '.###.', '#...#', '#...#', '.####', '....#', '...#.', '.##..' ],
			':': [ '.', '#', '.', '.', '.', '#', '.' ]
		};
		var PIXEL_SIZE = 3;

		var pixelText = function(text) {
			var x = 0, path = '';
			$.each(text.split(''), function(index, ch) {
				var glyph = GLYPHS[ch];
				if (!glyph)
					return;
				$.each(glyph, function(y, row) {
					for (var gx = 0; gx < row.length; gx++) {
						if (row.charAt(gx) == '#')
							path += 'M' + (x + gx) + ' ' + y + 'h1v1h-1z';
					}
				});
				x += glyph[0].length + 1;	// One column between characters, the last one holds the shadow
			});
			var height = 8;	// 7 rows plus the shadow
			return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ' + x + ' ' + height + '"' +
				' width="' + (x * PIXEL_SIZE) + '" height="' + (height * PIXEL_SIZE) + '" shape-rendering="crispEdges" aria-hidden="true">' +
				'<path fill="#3f3f3f" transform="translate(1 1)" d="' + path + '"/>' +
				'<path fill="currentColor" d="' + path + '"/></svg>';
		};

		var shownText = null;
		var showText = function(text) {
			if (text === shownText)
				return;
			shownText = text;
			clock
				.attr('aria-label', text)
				.html(text ? pixelText(text) : '');
		};

		// Between updates the clock runs on at the game's 20 ticks per second, once two updates show
		// the time is moving at all (it stands still in some dimensions, or with doDaylightCycle off).
		// An update arriving slightly behind the shown time (server below 20 TPS) holds the clock
		// rather than stepping it back a minute.
		var TICKS_PER_MS = 20 / 1000;
		var MAX_HOLD_TICKS = 400;
		var base = null;	// Last server time: { servertime, at, rate }
		var shown = null;	// Ticks on display

		var showTime = function() {
			if (timeout != null) {
				window.clearTimeout(timeout);
				timeout = null;
			}
			if (base == null) {
				clock.removeClass('day night');
				showText('');
				return;
			}
			var ticks = (base.servertime + (Date.now() - base.at) * base.rate) % 24000;
			if ((shown != null) && (base.rate > 0)) {
				var behind = (((shown - ticks) % 24000) + 24000) % 24000;	// Wraps at midnight
				if (behind < MAX_HOLD_TICKS)
					ticks = shown;
			}
			shown = ticks;
			var time = getMinecraftTime(Math.floor(ticks));
			clock
				.addClass(time.day ? 'day' : 'night')
				.removeClass(time.night ? 'day' : 'night');
			showText(formatTime(time));
			if (base.rate > 0) {
				timeout = window.setTimeout(showTime, 250);
			}
		};

		var setTime = function(servertime) {
			if (servertime >= 0) {
				var moving = (base != null) && (servertime != base.servertime);
				base = { servertime: servertime, at: Date.now(), rate: moving ? TICKS_PER_MS : 0 };
				if (!moving)
					shown = null;	// Show the server time as is
			}
			else {
				base = shown = null;
			}
			showTime();
		};

		$(dynmap).bind('worldupdated', function(event, update) {
			setTime(update.servertime);
		});
	}
	if(configuration.showweather) {
		var weather = $('<div/>')
			.addClass('weather')
			.appendTo(element);
				
		var setWeather = function(hasStorm, isThundering, time) {
			var daynight = (time > 23100 || time < 12900) ? "_day" : "_night";
			var cls = 'sunny';
			if (hasStorm) {
				cls = 'stormy';
				if (isThundering) {
					cls = 'thunder';
				}
			}
			weather
				.removeClass('stormy_day stormy_night sunny_day sunny_night thunder_day thunder_night')
				.addClass(cls + daynight);
		};
		
		$(dynmap).bind('worldupdated', function(event, update) {
			setWeather(update.hasStorm, update.isThundering, update.servertime);
		});
	}
	$(dynmap).bind('worldupdated', function(event, update) {
		var sunangle;
		var time = update.servertime;
		
		if(time > 23100 || time < 12900) {
			//day mode
			var movedtime = time + 900;
			movedtime = (movedtime >= 24000) ? movedtime - 24000 : movedtime;
			//Now we have 0 -> 13800 for the day period
			//Divide by 13800*2=27600 instead of 24000 to compress day
		    sunangle = ((movedtime)/27600 * 2 * Math.PI);
		} else {
			//night mode
			var movedtime = time - 12900;
			//Now we have 0 -> 10200 for the night period
			//Divide by 10200*2=20400 instead of 24000 to expand night
		    sunangle = Math.PI + ((movedtime)/20400 * 2 * Math.PI);
		}
		
		var moonangle = sunangle + Math.PI;

		if(time >= 0) {		
			sun.css('background-position', (-50 * Math.cos(sunangle)) + 'px ' + (-50 * Math.sin(sunangle)) + 'px');
			moon.css('background-position', (-50 * Math.cos(moonangle)) + 'px ' + (-50 * Math.sin(moonangle)) + 'px');
		}
		else {
			sun.css('background-position', '-150px -150px');
			moon.css('background-position', '-150px -150px');
		}
	});
};