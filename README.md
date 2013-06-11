Grief Prevention: TNG
=====================


"Minecraft, The final frontier.
These are the  voyages of the plugin Grief Prevention.
It's continuing mission, to protect strange new claims,
to seek out new exploits and new annoyances.
to boldly go, where no land protection plugin has gone before."

Grief Prevention: TNG is a a continuation of [Grief Prevention](https://github.com/ryanhamshire/GriefPrevention) by Ryan
Hamshire (bigscary).  The goal of this fork is to bring the codebase to a simpler more manageable foundation for
extensibility, without any immediate regard for backwards compatibility with data from the original plugin.


What's Different?
-----------------

 - Claims support arbitrary, persistent metadata.
 - A complete "Flag" system is built in to allow extension plugins to easily add new claim flags
 - all player movements are tracked and claim enter/exit events are fired.
 - Siege mode is gone, because I have no use for it and it can be implemented externally.
 - Spam protection is gone.  spam != grief. and GP wasn't super great at it.
 - The source code has been thoroughly cleaned up and reformatted. 
 - The command system has also seen the beginnings of a substantial overhaul, 
   all commands are now available as subcommands of a main `/griefprevention`
   command (also aliased to `/gp`).  some have also been combined into multi 
   purpose commands, for example `/restorenatureaggressive` is now 
   `/restorenature aggressive`.  Tab completion is available wherever possible.


What's Planned?
---------------

 - Probably a name change, there won't be much in common with the original when I'm through with it. :)
 - A more efficient persistence backend.
 - importers/converters for persistence backends.
 - more command consolidation
 - Suggestions welcome, but quite possibly ignored.

