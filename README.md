# Opportunity collector</p>
This tool will help simplify the process of searching & collecting opportunities on sites
 like sam.gov and grants.gov.

## Features
* Automatically saves configuration settings since they should rarely change
* Will fill out date ranges based on last query (i.e. search between last search date & today)
* Downloads as a spreadsheet
* Will attempt to alert you if an API key is due to expire

## Usage Notes
* Some sources require an API key to use
* Some sources have API usage limits. For this reason, some queries may take a few seconds-minutes to complete
* This tool is designed for a modern browser
* This tool looks best on monitors >= 1920x1080 (a regular FHD monitor)

## Dev Notes
This was built using:
* Java 8
* Gradle for the dependency management & build system
* Spring for the application framework
* Thymeleaf for some server side rendering
* Plain HTML/CSS/JS for client side
* AdminLTE as a HTML template

# License
MIT