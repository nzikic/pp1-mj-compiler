SVN:
https://xp-dev.com/svn/pp1domaci/trunk/nzikicpp1



jflex arguments:


-d src/nzikic/pp1 spec/nz-mjlexer.flex


cup arguments

-destdir src/nzikic/pp1 -parser MJParser spec/nz-parser.cup



terminal run :

disasm
java -cp mj-runtime.jar rs.etf.pp1.mj.runtime.disasm ../test/program.obj



terminal run :

Run
java -cp mj-runtime.jar rs.etf.pp1.mj.runtime.Run -debug ../test/program.obj
