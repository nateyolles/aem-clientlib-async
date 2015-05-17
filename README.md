# Clientlib Async Sample project

This project demonstrates how to create AEM clientlibs that can output 'async', 'defer' and 'onload' attributes on your HTML script elements.

## Project

This project was created using the [AEM project archetype](https://github.com/Adobe-Marketing-Cloud/aem-project-archetype). See the archetype's documentation for further information on modules, building, testing and Maven settings. The archetype may be a little heavy handed for this small example. The files you should pay attention to are as follows:

* /core/src/main/java/com/nateyolles/aem/clientlib/ClientLibUseObject.java
* /ui.apps/src/main/content/jcr_root/apps/clientlib-async-sample/components/structure/page/partials/headlibs.html
* /ui.apps/src/main/content/jcr_root/apps/clientlib-async-sample/sightly/templates/clientlib.html
* /ui.apps/src/main/content/jcr_root/apps/clientlib-async-sample/sightly/templates/graniteClientLib.html
* /ui.apps/src/main/content/jcr_root/etc/designs/clientlib-async-sample/clientlib-async-sample/js.txt
* /ui.apps/src/main/content/jcr_root/etc/designs/clientlib-async-sample/clientlib-async-sample/script.js

This project will work with or without [clientlib minification](http://localhost:4502/system/console/configMgr/com.day.cq.widget.impl.HtmlLibraryManagerImpl).

## How to build

You can build and deploy to a running AEM instance with default values of port *4502*, user *admin* and password *admin* with:

    mvn clean install -PautoInstallPackage

## View sample

After building and deploying, navigate to the sample page at [/content/clientlib-async-sample/en.html](http://localhost:4502/content/clientlib-async-sample/en.html). View the source to verify that the *clientlib-async-sample.js* script element has the *async* void attribute and the *onload* attribute with a value of *sayHello()*.

## Using the updated clientlibs

Use the clientlibs in your Sightly markup just as you would before (see [Sightly intro part 5: FAQ](http://blogs.adobe.com/experiencedelivers/experience-management/sightly-intro-part-5-faq/)), however, you will update the value of the data-sly-use parameter to point to the new clientlib components.

```
<head data-sly-use.clientLib="${'/apps/clientlib-async-sample/sightly/templates/clientlib.html'}">

<!--/* for css+js */-->
<meta data-sly-call="${clientLib.all @ categories='your.clientlib'}" data-sly-unwrap></meta>

<!--/* only js */-->
<meta data-sly-call="${clientLib.js @ categories='your.clientlib'}" data-sly-unwrap></meta>

<!--/* only css */-->
<meta data-sly-call="${clientLib.css @ categories='your.clientlib'}" data-sly-unwrap></meta>

</head>
```

## Using 'async', 'defer' and 'onload'

```
<!--/* async */-->
<meta data-sly-call="${clientLib.js @ categories='your.clientlib', loading='async'}" data-sly-unwrap></meta>

<!--/* defer */-->
<meta data-sly-call="${clientLib.js @ categories='your.clientlib', loading='defer'}" data-sly-unwrap></meta>

<!--/* defer and onload */-->
<meta data-sly-call="${clientLib.js @ categories='your.clientlib', loading='defer', onload='myFunction()'}" data-sly-unwrap></meta>
```

## TODO:

* Write unit tests
* Test with Themes
* Test with Channels