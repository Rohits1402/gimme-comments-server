fetch('https://gimme-comments-server.onrender.com/api/v1/initialization')
  .then((res) => res.json())
  .then((res) => {
    console.log('response is', res);
    const jsFiles = res.jsFiles;
    const cssFiles = res.cssFiles;

    elements = jsFiles.map((eachFileName) => {
      let script = document.createElement('script');
      script.src = `https://gimme-comments-server.onrender.com/build/static/js/${eachFileName}`;
      document.head.appendChild(script);
      return script;
    });
    elements = cssFiles.map((eachFileName) => {
      let link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = `https://gimme-comments-server.onrender.com/build/static/css/${eachFileName}`;
      document.head.appendChild(link);
      return link;
    });
  });
