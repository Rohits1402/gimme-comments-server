(function () {
  // Read the origin this script was served from, so the widget works against any
  // deployment without being recompiled. Must be read synchronously: by the time
  // the fetch below resolves, document.currentScript is already null.
  var BASE = new URL(document.currentScript.src).origin;

  fetch(BASE + '/api/v1/initialization')
    .then(function (res) {
      if (!res.ok) throw new Error('initialization returned ' + res.status);
      return res.json();
    })
    .then(function (manifest) {
      // Stylesheets first, so the widget is styled the moment it renders.
      (manifest.cssFiles || []).forEach(function (name) {
        var link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = BASE + '/build/static/css/' + name;
        document.head.appendChild(link);
      });

      (manifest.jsFiles || []).forEach(function (name) {
        var script = document.createElement('script');
        script.src = BASE + '/build/static/js/' + name;
        document.head.appendChild(script);
      });
    })
    .catch(function (err) {
      console.error('GimmeComments failed to load:', err);
    });
})();
